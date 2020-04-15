from html.parser import HTMLParser
from collections import namedtuple
import io

# Read and parse an HTNL document for conversion to ReST.
#

Tag = namedtuple('Tag', ['tag', 'attrs'])

SECTIONS = {
    'title': '#',
    'h1':    '=',
    'h2':    '-',
    'h3':    '~',
    'h4':    '_'
}

# These are the tags that accumulate text.
#
TEXT_TAGS = ['dd', 'div', 'dt', 'h1', 'h2', 'h3', 'li', 'p', 'pre', 'span', 'title']

class ParseError(ValueError):
    pass

def attr(tag, key):
    for k, v in tag.attrs:
        if k==key:
            return v

    return None

class HelpParser(HTMLParser):

    def __init__(self):
        super().__init__()

        # Keep track of which tag we're in.
        #
        self.tag_stack = []

        # # For HTML such as "<p><font>abc <em>xxx</em> xyz</font></p>", the <em>
        # # text doesn't append to the <font> text, it appends to the <p> text.
        # # Keep track of the most recent appendable text tag.
        # #
        # self.text_stack = []

        # This where the ReST text is output to.
        #
        self.buf = io.StringIO()

        # Example: a <p>, <pre>, etc paragraph may contain <i>, <sub> etc tags.
        # Since the paragraph level tags require different treatment, we gather
        # the text until the end tag, and interpret the <i> etc tags as we go.
        #
        self.gather = {}

    def push(self, tag):
        """Push a new tag onto the tag stack."""

        if tag.tag=='li' and self.top().tag=='li':
            # Starting a new <li> without closing the preceding <li>.
            #
            self.handle_endtag('li')

        self.tag_stack.append(tag)

        # if tag.tag in TEXT_TAGS:
        #     self.current_tag = tag

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
        tag = Tag(tag, attrs)
        self.push(tag)
        print(f'START: {tag}')

        if tag.tag in ['dl', 'ol', 'ul']:
            # Lists are, well, lists.
            # Gather their items in a list.
            #
            self.gather[tag.tag] = []
        elif tag.tag=='hr':
            self.gathertext('----\n\n')
        # elif tag.tag in ['dd', 'dt', 'li']:
        #     # A new list item.
        #     #
        #     outer_tag = self.top(-2)
        #     self.gather[outer_tag.tag].append(io.StringIO())
        else:
            self.gather[tag.tag] = io.StringIO()

        if tag.tag=='img':
            self.pop()
            print('** handle img here')

    def handle_endtag(self, tag):
        print(f'END  : {tag}')

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
        else:
            text = self.gather[tag]
            text.seek(0)
            text = text.read()
            print('@@endtag', tag, repr(text))

        if tag=='a':
            href = attr(start_tag, 'href')
            outer_tag = self.top()
            self.gathertag(outer_tag, f'`{text} <{href}>_')

        elif tag in ['caption', 'div', 'p']:
            self.gathertext(f'{text}\n\n')

        elif tag=='dl':
            # print('@@endtag dl', items)
            # import fred
            indent, pad = ('', '')
            for item in items:
                self.gathertext(f'{indent}{item}{pad}\n')
                indent, pad = ('  ', '\n') if indent=='' else ('', '')

        elif tag in ['em', 'strong']:
            outer_tag = self.top()
            self.gathertag(outer_tag, f'*{text}* ')

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
                self.gathertext(f'{indent}{item}\n\n')

        elif tag=='pre':
            self.gathertext('.. code-block:: text\n')
            for line in text.split('\n'):
                self.gathertext(f'  {line.rstrip()}\n')

        elif tag in SECTIONS:
            # We don't need to write the title, it doesn't really belong in the text.
            #
            if tag!='title':
                mup = SECTIONS[tag] * len(text)
                self.gathertext(f'{text}\n{mup}\n\n')

        elif tag=='span':
            outer_tag = self.top()
            print('@@span', start_tag, text)
            if attr(start_tag, 'class') in ['mono', 'tt']:
                self.gathertag(outer_tag, f' ``{text}`` ')
            else:
                self.gathertag(outer_tag, text)

        elif tag=='sub':
            outer_tag = self.top()
            self.gathertag(outer_tag, f'\\ :sub:`{text}`\\ ')

        elif tag in ['dd', 'dt', 'li']:
            outer_tag = self.top()
            print('@@item', tag, outer_tag)
            self.gather[outer_tag.tag].append(text)
            # # TODO: ul or ol?
            # #
            # indent = '- '
            # for line in text.split('\n'):
            #     self.gathertext(f'{indent}{line}\n')
            #     indent = '  '

        elif tag in ['table', 'td', 'th', 'tr']:
            print('-- Ignoring table stuff for now')

        elif tag in ['body', 'center', 'head', 'html', 'ol', 'script', 'sub', 'tbody', 'thead', 'ul']:
            # Don't care about these at endtag time.
            #
            pass

        else:
            raise ParseError(f'Unrecognised end tag "{tag}""')

    def handle_data(self, data):
        # Which HTML tag am I currently within?
        #
        tag = self.top()

        if tag and tag.tag=='pre':
            lines = data
        else:
            # Remove any indents.
            #
            lines = ' '.join(line.strip() for line in data.split('\n')).strip()
        print(f'DATA : {lines}')

        if tag is None:
            pass
        elif tag.tag=='a':
            self.gathertag(tag, lines)
        elif tag.tag in ['body', 'dl', 'html', 'head', 'hr', 'img', 'link', 'meta', 'ol', 'tbody', 'thead', 'ul']:
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
        elif tag.tag in ['table', 'td', 'th', 'tr']:
            print('-- Ignoring table stuff for now')
        else:
            raise ParseError(f'Unrecognised data tag: {tag}')

    def handle_startendtag(self, tag, attrs):
        t = Tag(tag, attrs)
        print(f'TAG  : {t}')


def parse_html(help_html):
    with open(help_html) as f:
        content = ''.join(f.readlines())

    hp = HelpParser()
    hp.feed(content)
    hp.close()

    return hp.get_rest()
