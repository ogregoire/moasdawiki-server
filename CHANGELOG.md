# Changelog

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
