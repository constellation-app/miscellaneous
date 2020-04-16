import argparse
from pathlib import Path
import xml.etree.ElementTree as ET
import datetime

import pprint

from parsehelp import parse_html

# Convert NetBeans HelpSet files to Sphinx.
#
# Find all the package-info.java files that contain '@HelpSetRegistration'.
# Get the name of the helpset xml and parse that to get the map and toc values.
# Merge the tocs into a single toc.

ITEMS = '__items__'

INDEX_RST = '''.. Frogger documentation master file, created by
   {} on {}.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

{}
{}

.. toctree::
    :maxdepth: 2
    :caption: Contents:

{}

Indices and tables
==================

* :ref:`genindex`
* :ref:`search`

'''

def helpsets(dir):
    """Yield NetBeans HelpSet marker files."""

    for pinfo in dir.rglob('package-info.java'):
        with pinfo.open() as f:
            for line in f.readlines():
                if line.startswith('@HelpSetRegistration'):
                    q1 = line.index('"')
                    q2 = line.index('"', q1+1)
                    name = line[q1+1:q2]
                    hs = pinfo.with_name(name)
                    yield hs

def parse_helpset(hs):
    """Parse a -hs.xml helpset file."""

    hs_xml = ET.parse(str(hs))
    root = hs_xml.getroot()
    # print(root)

    refs = {}
    for child in root:
        if child.tag=='maps':
            mapref = child.find('mapref')
            location = mapref.attrib['location']
            # print(location)
            refs['location'] = location
        elif child.tag=='view':
            type = child.find('type').text
            data = child.find('data').text
            refs[type] = data

    return refs

def parse_map(hs, m):
    """Parse a -map.html helpset mapping file."""

    m = hs.with_name(m)
    m_xml = ET.parse(str(m))
    root = m_xml.getroot()

    maps = {}
    for child in root:
        assert child.tag=='mapID'
        target = child.attrib['target']
        url = child.attrib['url']
        maps[target] = hs.with_name(url)

    return maps

def parse_toc(hs, toc):
    """Parse a -toc.xml helpset table-of-contents file.

    Slightly trickier, because there are levels of <tocitem> tags.
    Each level has a 'text' attrib, but only the leaves have
    a 'target' attrib'.

    Just do it recursively.
    """

    # Leaf items are collected in a list.
    #

    def toc_level(tocs, root):
        for item in root.findall('tocitem'):
            text = item.attrib['text']
            if 'target' in item.attrib:
                # This is a leaf referencing a help target.
                #
                tocs[ITEMS].append((text, item.attrib['target']))
            else:
                if text not in tocs:
                    tocs[text] = {ITEMS:[]}
                toc_level(tocs[text], item)

                # If there are no leaves at this level, remove the empty list.
                #
                if not tocs[text][ITEMS]:
                    del tocs[text][ITEMS]

    tocs = {}

    toc = hs.with_name(toc)
    toc_xml = ET.parse(str(toc))
    root = toc_xml.getroot()
    toc_level(tocs, root)

    return tocs

def merge_tocs(toc_list):
    """Merge a list of tocs into a single toc.

    Each level of toc is a dict with two optional keys:
    * name - the name of the level, contains a dict of the next level
    * '__items__' - a list of (name,target) tuples.

    Recursive, obviously.
    """

    def merge_level(merged, level):
        for k,v in level.items():
            if k==ITEMS:
                if ITEMS not in merged:
                    merged[ITEMS] = []
                merged[ITEMS].extend(v)
            else:
                if k not in merged:
                    merged[k] = {}
                merge_level(merged[k], v)

    toc1 = {}
    for toc in toc_list:
        merge_level(toc1, toc)

    return toc1

def generate_pages(outdir, merged_tocs, merged_maps):
    """Generate documentation in a proper directory hierarchy."""

    def simple_name(name):
        return ''.join(c if '0'<=c<='9' or 'a'<=c<='z' else '_' for c in name.lower())

    def ensure_dir(dir, category):
        d = dir / category
        if not d.is_dir():
            d.mkdir()

    def tree(category, toc, levels):
        level = '/'.join(levels)
        ensure_dir(outdir, level)
        if '__items__' in toc:
            for doc in toc['__items__']:
                in_html = merged_maps[doc[1]]
                out_rst = outdir / level / Path(in_html).with_suffix('.rst').name
                yield level, category, in_html, out_rst
        for sub_category in toc:
            cat = simple_name(sub_category)
            if sub_category!='__items__':
                sublevel = levels[:]
                sublevel.append(cat)

                # Yield the index of the next level down.
                #
                sl = '/'.join(sublevel)
                yield level, category, None, outdir / sl / 'index.rst'

                # Recursively yield the next level down.
                #
                yield from tree(sub_category, toc[sub_category], sublevel)

    yield from tree('CONSTELLATION', merged_tocs, [])

if __name__=='__main__':

    def dir_req(s):
        """Require this parameter to be a directory, and convert it to a Path instance."""

        p = Path(s)
        if not p.is_dir():
            raise argparse.ArgumentTypeError('Must be a directory')

        return p

    parser = argparse.ArgumentParser(description='Process existing HTML to ReST.')
    parser.add_argument('--indir', type=dir_req, required=True, help='Directory containing NetBeans help')
    parser.add_argument('--outdir', type=dir_req, required=True, help='Output directory tree')

    args = parser.parse_args()
    print(args.indir, args.outdir)

    merged_maps = {}
    toc_list = []
    for hs in helpsets(args.indir):
        # print(hs)

        refs = parse_helpset(hs)
        # print(refs)

        maps = parse_map(hs, refs['location'])
        # print(maps)
        for target, url in maps.items():
            if target in merged_maps:
                raise ValueError(f'Target {target} already found')
            merged_maps[target] = url

        toc = parse_toc(hs, refs['javax.help.TOCView'])
        # pprint.pprint(toc)
        toc_list.append(toc)

        # break

    # pprint.pprint(toc_list)

    merged_tocs = merge_tocs(toc_list)
    # pprint.pprint(merged_tocs)
    print()
    # print(merged_tocs.keys())
    print()
    # print(merged_maps)

    print()

    # We need an index.rst in each directory.
    # Keep track of the levels so we can generate them at the end.
    #
    levels = {}
    pages = []
    for level, category, in_html, out_rst in generate_pages(args.outdir / 'pages', merged_tocs, merged_maps):
        lc = level,category
        if lc not in levels:
            levels[lc] = []
        levels[lc].append(out_rst)

        if in_html:
            rest = parse_html(in_html)
            with open(out_rst, 'w', encoding='utf8') as f:
                f.write(rest)

        pages.append(f'    pages/{level}/{out_rst.name}')

    # Create index.rst at each level.
    # Each index.rst must have a reference to the index files below it.
    #
    now = datetime.datetime.now().isoformat(' ')[:19]
    for (level, category), rst_files in levels.items():

        pages = []
        for page in rst_files:
            p = Path(page)
            if p.name=='index.rst':
                entry = f'{p.parent.name}/index'
            else:
                entry = p.stem
            pages.append(f'    {entry}')

        mup = '=' * len(category)
        contents = INDEX_RST.format(__file__, now, category, mup, '\n'.join(pages))
        with open(args.outdir / 'pages' / level / 'index.rst', 'w') as f:
            f.write(contents)

    # pprint.pprint(merged_tocs)
