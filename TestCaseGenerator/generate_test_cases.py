import fnmatch
import os
import re
import datetime

"""
When run from the constellation folder this will generate a series of generic tests to run using the MarkDown format. This is still a WIP but the idea would be to output this into a TEST_SUITES.md file.
"""


# https://stackoverflow.com/questions/29916065/how-to-do-camelcase-split-in-python
def camel_case_split(identifier):
    return re.sub('([A-Z][a-z]+)', r' \1', re.sub('([A-Z]+)', r' \1', identifier)).split()


def extract_menu(code):
    return re.sub('".*', '', re.sub('.*path = "Menu/', '', code))


def extract_menu_entry(code):
    return re.sub('".*', '', re.sub('.*=', '', code))


if __name__ == "__main__":
    rootdir = os.getcwd()
    viewPattern = "*View"

    print("""# Testing Constellation
This testing suite was generated using https://github.com/constellation-app/miscellaneous/blob/master/TestCaseGenerator/generate_test_cases.py
""")

    views = ()

    for dir in os.listdir(rootdir):
        if fnmatch.fnmatch(dir, viewPattern):
            moduleName = ""

            for word in camel_case_split(dir):
                if "Core" != word:
                    moduleName += word + " "
            views += (moduleName, )

    print("## Views")

    for view in sorted(views):
        print("""
### Testing """ + view + """

* [ ] Testing of the """ + view + """ is in progress.

1. Click on Views > """ + view + """
1. Are there any errors or exceptions thrown?
1. If the view is enabled, can you click on any buttons (if applicable)?
1. Did each button work as intended?
1. Are there any exceptions or errors thrown?
1. If the view is enabled, can you click on any drop down lists (if applicable)?
1. Did each drop down list work as intended?
1. Are there any exceptions or errors thrown?
1. Close the View
1. Click on File > New Graph
1. Open the """ + view + """ again
1. Are there any errors or exceptions thrown?
1. Is the view still disabled? If so this is likely a problem!
1. Click the buttons
1. Did each button work as intended?
1. Are there any errors or exceptions thrown?
1. Click on drop down lists
1. Did each drop down list work as intended?
1. Are there any errors or exceptions thrown?

* [ ] Completed testing the """ + view)

    actions = {}
    plugins = {}  # TODO test this and de-conflict with Action entries as not to double up

    for folder, dirs, files in os.walk(rootdir):
        for file in files:
            fullpath = os.path.join(folder, file)
            if file.endswith("Action.java"):
                actions[file] = fullpath
            elif file.endswith("Plugin.java"):
                plugins[file] = fullpath

    print("## Menus Options")

    for action in sorted(actions):
        menu = ''
        with open(actions.get(action), 'r') as f:
            for line in f:
                if 'path = "Menu/' in line:
                    menu = extract_menu(line).rstrip()
                    menu += ' > '
                if '("CTL_' in line:
                    menu += extract_menu_entry(line).rstrip()
        print("""
### Testing """ + menu + ' (' + action + ')' + """

* [ ] Testing of the """ + menu + """ is in progress.

1. Try to click on """ + menu + """
1. If the menu is disabled this may be as per design, can you confirm this?
1. Click on File > New Graph
1. Click on """ + menu + """
1. Did the menu work as intended?
1. Are there any exceptions?

* [ ] Completed testing """ + menu)

    print("\n\n--- This was generated on " + datetime.datetime.now().strftime("%c"))
