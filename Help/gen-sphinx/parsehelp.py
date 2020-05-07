from html.parser import HTMLParser
from collections import namedtuple
import io

# Read and parse an HTNL document for conversion to ReST.
#

Tag = namedtuple('Tag', ['tag', 'attrs'])

SECTIONS = {
    'title': '=',
    'h1':    '-',
    'h2':    '`',
    'h3':    ':',
    'h4':    "'"
}

class ParseError(ValueError):
    pass

def attr(tag, key):
    """Retrieve an attribute value from a tag."""

    for k, v in tag.attrs:
        if k==key:
            return v

    return None

def normalise(text):
    """Convert multiple spaces, new lines to a single space; remove leading/trailing spaces."""
    return ' '.join(text.strip().split())

def table_row(text_list):
    """Make a CSV row from a list."""
    return ','.join(f'"{text}"' for text in text_list)

class HelpParser(HTMLParser):

    def __init__(self):
        super().__init__()

        # Keep track of which tag we're in.
        #
        self.tag_stack = []

        # This where the ReST text is output to.
        #
        self.buf = io.StringIO()

        # Example: a <p>, <pre>, etc paragraph may contain <i>, <sub> etc tags.
        # Since the paragraph level tags require different treatment, we gather
        # the text until the end tag, and interpret the <i> etc tags as we go.
        #
        self.gather = {}

        # Resources such as images need to be kept with the document.
        # Keep track tuples of relative (from, to) filenames.
        # The caller can use this to copy what needs to be copied.
        #
        self.resources = []

        # All images will be inline using substitution references,
        # so track the img substitutions.
        #
        self.imgs = {}

    def push(self, tag):
        """Push a new tag onto the tag stack."""

        if tag.tag=='li' and self.top().tag=='li':
            # Starting a new <li> without closing the preceding <li>.
            #
            self.handle_endtag('li')

        self.tag_stack.append(tag)

    def pop(self):
        """Pop and return the top tag from the tag stack.

        Since this is HTML, we have no pretensions of correctness,
        so allow for unmatched tags leading to an empty stack.
        """

        return self.tag_stack.pop() if self.tag_stack else None

    def top(self, index=-1):
        return self.tag_stack[index] if self.tag_stack else None

    def gathertext(self, s):
        """Gather text into the ReST buffer."""

        print(s, file=self.buf, end='')

    def gathertag(self, tag, s):
        """Gather text into the specified tag."""

        gather = self.gather[tag.tag]
        print(s, file=gather, end='')

    def get_rest(self):
        self.buf.seek(0)
        return self.buf.read()

    def handle_starttag(self, tag, attrs):
        """Called to start a new HTML tag."""

        tag = Tag(tag, attrs)
        self.push(tag)
        # print(f'&START: {tag}')

        if tag.tag in ['dl', 'ol', 'ul']:
            # Lists are, well, lists.
            # Gather their items in a list.
            #
            self.gather[tag.tag] = []
        elif tag.tag=='hr':
            # No end tag for <hr>, so do all the work here.
            #
            self.gathertext('----\n\n')

        elif tag.tag=='img':
            # No end tag for <img>, so do all the work here.
            #
            self.pop()
            src = attr(tag, 'src')
            src2 = src.replace('/', '-').replace('..', '--')
            alt = attr(tag, 'alt')
            width = attr(tag, 'width')
            height = attr(tag, 'height')

            img = f'.. image:: {src2}\n'
            if width and height:
                img += f'   :width: {width}px\n   :height: {height}px\n'
            if alt:
                img += f'   :alt: {alt}\n'

            self.imgs[src2] = img
            outer_tag = self.top()
            self.gathertag(outer_tag, f'|{src2}| ')

            self.resources.append((src, src2))

        elif tag.tag=='table':
            # Store table data in a list of lists.
            #
            self.gather[tag.tag] = []
            self.table_has_header = False
        elif tag.tag=='tr':
            # Start a new list for a new row.
            #
            self.gather['table'].append([])

        else:
            self.gather[tag.tag] = io.StringIO()

            if tag.tag=='thead':
                self.table_has_header = True

    def handle_endtag(self, tag):
        # print(f'&END  : {tag}')

        # If we're closing <html>, I don't care any more.
        # HTML is just broken.
        #
        if tag=='html':
            return

        start_tag = self.pop()

        if tag=='body':
            while start_tag.tag!='body':
                start_tag = self.pop()
        elif tag=='head':
            while start_tag.tag!='head':
                start_tag = self.pop()
        elif tag=='ol':
            while start_tag.tag!='ol':
                start_tag = self.pop()
        elif tag=='ul':
            while start_tag.tag!='ul':
                start_tag = self.pop()
        elif tag!=start_tag.tag:
            while start_tag.tag=='br':
                start_tag = self.pop()

        # Check that the end tag and stacked tag match (except where HTML is broken).
        #
        if tag!=start_tag.tag:
            raise ParseError(f'End tag "{tag}" != stacked tag {start_tag}')

        if tag in ['dl', 'ol', 'ul']:
            # Collect the list of items.
            #
            items = [item for item in self.gather[tag]]
            # for item in self.gather[tag]:
            #     item.seek(0)
            #     items.append(item.read())
        elif tag=='table':
            # Deal with the table's list of lists.
            # Use csv-table format because it's the easiest.
            #
            table = '.. csv-table::\n'
            if self.table_has_header:
                row = self.gather[tag].pop(0)
                table += f'   :header: {table_row(row)}\n'
                table += '\n'
                for row in self.gather[tag]:
                    table += f'   {table_row(row)}\n'

                self.gathertext(f'{table}\n')
            # print('TABLE', self.gather[tag])
            return
        elif tag=='tr':
            pass
        else:
            text = self.gather[tag]
            text.seek(0)
            text = text.read()
            # print('@@endtag', tag, repr(text))

        if tag=='a':
            href = attr(start_tag, 'href')
            outer_tag = self.top()
            self.gathertag(outer_tag, f'`{text} <{href}>`_')

        elif tag in ['caption', 'div', 'p']:
            text = normalise(text)
            self.gathertext(f'{text}\n\n')

        elif tag=='dl':
            indent, pad = ('', '')
            for item in items:
                self.gathertext(f'{indent}{item}{pad}\n')
                indent, pad = ('  ', '\n') if indent=='' else ('', '')

        elif tag in ['em', 'strong']:
            outer_tag = self.top()
            self.gathertag(outer_tag, f'*{text}*')

        elif tag in ['font', 'strong']:
            outer_tag = self.top()
            self.gathertag(outer_tag, text)

        elif tag in ['ol', 'ul']:
            # Collect the list of items.
            # TODO: sublists
            # TODO: ol vs ul
            #
            # print('@@endtag', tag, items)
            indent = '* '
            for item in items:
                self.gathertext(f'{indent}{normalise(item)}\n')
            self.gathertext('\n')

        elif tag=='pre':
            self.gathertext('.. code-block:: text\n')
            for line in text.split('\n'):
                self.gathertext(f'  {line.rstrip()}\n')

        elif tag in SECTIONS:
            text = text.strip()
            # We don't need to write the title, it doesn't really belong in the text.
            #
            if tag!='title':
                mup = SECTIONS[tag] * len(text)
                self.gathertext(f'{text}\n{mup}\n\n')

        elif tag=='span':
            outer_tag = self.top()
            # print('@@span', start_tag, text)
            if attr(start_tag, 'class') in ['mono', 'tt']:
                self.gathertag(outer_tag, f'``{text}``')
            else:
                self.gathertag(outer_tag, text)

        elif tag=='sub':
            outer_tag = self.top()
            self.gathertag(outer_tag, f'\\ :sub:`{text}`\\ ')

        elif tag in ['dd', 'dt', 'li']:
            outer_tag = self.top()
            self.gather[outer_tag.tag].append(text)
            # # TODO: ul or ol?
            # #
            # indent = '- '
            # for line in text.split('\n'):
            #     self.gathertext(f'{indent}{line}\n')
            #     indent = '  '

        elif tag in ['td', 'th']:
            self.gather['table'][-1].append(normalise(text))
            if tag=='th':
                self.table_has_header = True

        elif tag in ['body', 'center', 'head', 'html', 'ol', 'script', 'sub', 'tbody', 'thead', 'tr', 'ul']:
            # Don't care about these at endtag time.
            #
            pass

        else:
            raise ParseError(f'Unrecognised end tag "{tag}""')

    def handle_data(self, data):
        # Which HTML tag am I currently within?
        #
        tag = self.top()

        # if tag and tag.tag=='pre':
        #     lines = data
        # else:
        #     # Remove any indents.
        #     #
        #     lines = ' '.join(line.strip() for line in data.split('\n')).strip()
        lines = data
        # print(f'&DATA : {lines}')

        if tag is None:
            pass
        elif tag.tag=='a':
            self.gathertag(tag, lines)
        elif tag.tag in ['body', 'dl', 'html', 'head', 'hr', 'img', 'link', 'meta', 'ol', 'ul']:
            # Don't care about these tags when handling data.
            #
            pass
        elif tag.tag in ['dt', 'dd', 'li']:
            self.gathertag(tag, lines)
        elif tag.tag=='br':
            self.gathertag(tag, '\n')
        elif tag.tag=='em':
            self.gathertag(tag, lines)
        elif tag.tag in ['caption', 'center', 'div', 'p', 'pre'] or tag.tag in SECTIONS:
            self.gathertag(tag, lines)
        elif tag.tag=='font':
            outer_tag = self.top(-2)
            self.gathertag(outer_tag, f' {lines} ')
        elif tag.tag=='span':
            self.gathertag(tag, lines)
            # outer_tag = self.top(-2)
            # if 'class' in tag.attrs and tag.attrs['class'] in ['mono', 'tt']:
            #     self.gathertag(outer_tag, f' ``{lines}`` ')
            # else:
            #     self.gathertag(outer_tag, f' {lines} ')
        elif tag.tag=='strong':
            # outer_tag = self.top(-2)
            # self.gathertag(outer_tag, f'**{lines}**')
            self.gathertag(tag, lines)
        elif tag.tag=='sub':
            # self.gathertag(tag, f'\\ :sub:`{lines}`\\ `')
            self.gathertag(tag, lines)
        elif tag.tag=='ul':
            # TODO: ul or ol?
            #
            pass
        elif tag.tag in ['table', 'tbody', 'thead', 'tr']:
            # Don't care about data at this level.
            #
            pass
        elif tag.tag in ['td', 'th']:
            self.gathertag(tag, lines)
        else:
            raise ParseError(f'Unrecognised data tag: {tag}')

    def handle_startendtag(self, tag, attrs):
        t = Tag(tag, attrs)
        # print(f'&TAG  : {t}')

    def close(self):
        super().close()

        # Append any image substitution definitions.
        #
        for img, subst_def in self.imgs.items():
            subst_def = subst_def.replace('..', f'.. |{img}|', 1)
            self.gathertext(f'{subst_def}\n')

def parse_html(help_html):
    with open(help_html) as f:
        content = ''.join(f.readlines())

    hp = HelpParser()
    hp.feed(content)
    hp.close()

    return hp.get_rest(), hp.resources
