import argparse
from pathlib import Path
import xml.etree.ElementTree as ET

import pprint

# Convert NetBeans HelpSet files to Sphinx.
#
# Find all the package-info.java files that contain '@HelpSetRegistration'.
# Get the name of the helpset xml and parse that to get the map and toc values.
# Merge the tocs int oa single toc.

ITEMS = '__items__'

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
        maps[target] = url

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

    toc_list = []
    for hs in helpsets(args.indir):
        # print(hs)
        refs = parse_helpset(hs)
        # print(refs)

        maps = parse_map(hs, refs['location'])
        # print(maps)

        toc = parse_toc(hs, refs['javax.help.TOCView'])
        # pprint.pprint(toc)
        toc_list.append(toc)

        # break

    # pprint.pprint(toc_list)

    merged = merge_tocs(toc_list)
    pprint.pprint(merged, width=132)
    print()
    print(merged.keys())