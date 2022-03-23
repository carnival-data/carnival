
# <a name="github-pages-site"></a> Github Pages

The main carnival website is a github pages is hosted by Github Pages. 
The static content is automatically built by and deployed to the `gh_pages` branch by a github action, which combines the content of the `/docs` directory and API documentation automatically generated from running groovydoc on the source code.
In most cases documentation can just be modified by **adding or manipulating .md files in the `/docs` directory** or **adding javadoc formatted comments in the code**.



## Building Jekyll Documentation Locally
[Github Pages](https://docs.github.com/en/pages/setting-up-a-github-pages-site-with-jekyll/about-github-pages-and-jekyll) makes use of [jekyll](https://jekyllrb.com). 
Although usually not necessary, below are instructions to build the Jekyll documentation locally.

See the [jekyll docs](https://jekyllrb.com/docs/) for jekyll installation and usage instructions.

### Prerequisites
- Install [Ruby](https://www.ruby-lang.org/en/)
- Install [Jekyll](https://jekyllrb.com)

### Building
To build the documentation:

```
cd docs
bundle exec jekyll clean
bundle exec jekyll build
```

### Local Jekyll Server

To run the Jekyll server locally:

```
bundle exec jekyll serve
```
