import os
import fnmatch
import re

"""
When run from the constellation folder this will generate a series of generic tests to run using the MarkDown format. This is still a WIP but the idea would be to output this into a TEST_SUITES.md file.
"""

# https://stackoverflow.com/questions/29916065/how-to-do-camelcase-split-in-python
def camel_case_split(identifier):
  return re.sub('([A-Z][a-z]+)', r' \1', re.sub('([A-Z]+)', r' \1', identifier)).split()
    
if __name__ == "__main__":
    rootdir = os.getcwd()
    viewPattern = "*View"

    for dir in os.listdir(rootdir):
        if fnmatch.fnmatch(dir, viewPattern):
            moduleName=""

            for word in camel_case_split(dir):
                if "Core" != word:
                    moduleName+=word + " "

            print("""
## Testing """ + moduleName + """

1. Open Constellation
1. Click on Views > """ + moduleName + """
1. Are there any errors or exceptions thrown?
1. If the view is enabled, can you click on any buttons?
1. Are there any exceptions or errors thrown?
1. If the view is enabled, can you click on any drop down lists?
1. Are there any exceptions or errors thrown?
1. Close the View
1. Click on File > New Graph
1. Open the """ + moduleName + """ again
1. Are there any errors or exceptions thrown?
1. Is the view still disabled? If so there is a problem!
1. Click on buttons
1. Are there any errors or exceptions thrown?
1. Click on drop down lists
1. Are there any errors or exceptions thrown?""")
