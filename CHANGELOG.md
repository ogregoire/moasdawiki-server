# Changelog

## 2.6.3 (2021-06-15)

- Syntax highlighting for YAML code
- Extend Java syntax highlighting for annotations
- Improve XML and HTML syntax highlighting

## 2.6.2 (2021-06-13)

- Syntax highlighting for ini code
- Upgrade build dependencies

## 2.6.1 (2021-05-22)

- Change License to AGPL-3.0-only

## 2.6.0 (2021-05-13)

- Make GPL-3.0-only licensing more clear

## 2.5.3 (2021-05-11)

- More strict XML parsing
- Small code improvements

## 2.5.2 (2021-02-28)

- Find also words that start with the search string

## 2.5.1 (2021-01-31)

- Bugfix: Add wiki page name to search index

## 2.5.0 (2021-01-31)

- Enhance search index to full words
- Lazy load search index
- Introduce search ignore list
- Increase file upload size to 10 MB
- Add support for SVG images
- Refactoring

## 2.4.4 (2021-01-18)

- Search results: Don't scan wiki page content in App to speed up search
- Search results: Sort by relevance and page name
- Search results: Hide relevance points

## 2.4.3 (2021-01-04)

- Update README.md
- Code changes for App support
- Bugfix: Reset enumeration counting
- Fix primitive array annotation warnings
- Update copyright year

## 2.4.2 (2020-12-28)

- Refactoring cache updates for App support
- Optimize log output

## 2.4.1 (2020-12-27)

- Small changes for App support
- Bug fixes

## 2.4.0 (2020-12-26)

- Internationalize wiki editor and synchronization page
- Upgrade to HTML 5
- HTML pages specify language code
- Refactoring: Remove plugin support
- Refactoring: Remove RepositoryService interface
- Refactoring: Remove WikiService interface
- Refactoring: Remove WikiService file content cache
- Clean up config file
- Upgrade build dependencies
- Update JavaDoc comments

## 2.3.3 (2020-10-11)

- Add SearchService read-only mode for App

## 2.3.2 (2020-10-11)

- Extend wiki search to find also numbers
- Small code changes

## 2.3.1 (2020-06-21)

- Extend wiki search to find also upper case words with 3 characters

## 2.3.0 (2020-06-14)

- Speed up full text search by a search index
- Do not store last synchronization timestamp in session list to avoid file modification on every synchronization attempt
- Bugfix: Exclude session list from synchronization with App
- Refactorings

## 2.2.0 (2020-02-08)

- Speed up full text search by searching only in the page titles in the first step
- Add unit tests
- Refactorings

## 2.1.0 (2020-01-04)

- Code refactored to use the new language features of Java 1.8
- Replaced JAXB implementation by my own one to keep it running on future Java releases
- Added `@Nullable` and `@NotNull` annotations, fixed potential bugs
- RepositoryService and WikiService refactored, rewrote parts of the implementation
- Removed UDP broadcast support as it didn't really work for the app 
- Moved app sources to a separate project
- Updated copyright
- Ant build script replaced by Gradle, restructured repository folders
- Published sources on GitLab
- Using GitLab CI

## 2.0.9 (2019-01-03)

- HTML layout changed to border-box
- App: targetSdkVersion updated due to recent Google requirements
- App: Doesn't use an internal TCP port any more. Thus, the app requires no Internet permission for normal usage, it is only necessary for synchronisation.
- App (versionCode 10)
- App (versionCode 11)
- App (versionCode 12)
  - App: Bugfix: Fixed broken search
- App 2.0.9.2 (versionCode 13)
  - App: Bugfix: Page titles with space or special characters couldn't be shown

## 2.0.8 (2018-02-05)

- App can be closed by pressing the back button twice.
- Bugfix: Children pages were not restored after editing a wiki page.
- App (versionCode 9)

## 2.0.7 (2017-05-28)

- Bugfix: App: Editor icons got visible on scrolling
- App (versionCode 7)

## 2.0.6 (2016-11-28)

- Bugfix: JavaScript was not loaded in English version
- Bugfix: Some missing translations added
- Bugfix: App: Edit symbol was still available and active
- App (versionCode 6)

## 2.0.5 (2016-11-22)

- Initial App version (versionCode 5)

## 2.0.1 (2016-11-22)

- Bugfix: Error in path handling in Windows, media files were not found

## 2.0.0 (2016-11-21)

- MoasdaWiki as Android App (versionCode 2)
  - App provides a copy for mobile usage
  - Synchronizes with a MoasdaWiki server
  - Optimized for small display resolution, uses integrated navigation of app
- New syntax element class WikiTag
- New plugin for synchronizing with the app
  - Status view and grant app access via the synchronization wiki page
  - UDP broadcast search by app to find the server
- Repository access supports both, app assets and Android Internal Storage
- Added cache files to speed up app startup
- New GUI design
- Bugfix: Firefox requests sometimes got stuck for some seconds (speculative requests).

## 1.12 (2015-01-18)

- `@@` tag now supports syntax highlighting for Java, HTML, XML, and Properties
- Rendering of multi line code sections (`@@...@@`) improved to support copy & paste preserving line breaks
- Formatting of date and time now in English format, can be changed by message file
- Source code now requires Java 1.7, updated documentation
- Bugfix: Image URLs were not escaped correctly, e.g. if URL contains the character `+`
- Bugfix: Index pages did not show their last modification timestamp

## 1.11 (2014-06-21)

- Translated project into English
- Bugfix: Page links with special character `!` didn't work
- Bugfix: Wiki editor had problems with special characters in page name
