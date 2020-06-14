# MoasdaWiki Server

## Description

MoasdaWiki is a web based knowledge management application that allows you to
create and modify its content by a web browser.

The latest version is available at https://moasdawiki.net/.

### Key features

- Easy usability
- No setup necessary, can be run from a USB stick
- No special system requirements like database or application server
- Runs on any system with a Java VM 1.8 or higher
- Extendable by plugins
- App available, always have a copy of the Wiki content on your mobile device
- Free of charge, open source

### Feature list

- Store pages as text files on the file system, no database required
- Fast and powerful full text search, supports regular expressions
- Built-in lightweight web server, no external web server required
- Powerful syntax, includes tables and images
- Page templates to create uniform looking pages
- Full customizable graphical user interface
  - Navigation panel can be modified
  - Page layout for view in browser and for printing is defined by CSS stylesheet
  - Page header and footer can be modified
  - Customizable HTML header, e.g. to include your own CSS stylesheets and JavaScript libraries
  - Configuration file withs lots of options
- Extensible via plugins
- Internationalization via message files
- List of recently modified and viewed pages

## Installation
### Prerequisites

MoasdaWiki server requires Java 1.8 or higher and is running on several
operating systems and platforms.

### Download / Build

You can **download** MoasdaWiki server from https://gitlab.com/moasdawiki/moasdawiki-server/-/tags.
Unzip the file `moasdawiki-server-<version>.zip` in any folder.
No special installation steps necessary.

Alternatively, you can clone and **build** the project locally:
```
gradle build
```
Afterwards, you will find the target ZIP file in the folder ```build/distributions/```.

### Running manually

Ways to start the Wiki server:

#### Via console (Linux/Windows)

1. Open a console
2. Go to folder that contains `moasdawiki-server-<version>.jar`
3. `java -jar moasdawiki-server-<version>.jar repository-en`

#### Via shortcut icon (Windows)

1. Create a new shortcut (right mouse button on desktop &rarr; New &rarr; Shortcut)
2. Location: `javaw.exe -jar moasdawiki-server-<version>.jar repository-en`
3. Run in: Folder that contains `moasdawiki-server-<version>.jar repository-en`
4. Double click on the shortcut

MoasdaWiki expects the repository with the Wiki pages in the sub folder `repository-en`.
To choose the repository with German language use `repository-de` instead.

### Running as a Linux daemon

#### Using systemd (since Ubuntu 15.10)

1. Check if systemd is running
   ```
   cat /proc/1/comm
   # must return "systemd"
   ```
2. Install required packages
   - `openjdk-8-jre` (any Java VM 1.8 or higher)
   - `jsvc` (see http://jakarta.apache.org/commons/daemon/)
   - `libcommons-daemon-java`
3. Open files `moasdawiki.sh` and `moasdawiki.service` with a text editor and adjust the paths.
4. Mark files as executable
   ```
   chmod a+x moasdawiki.service
   chmod a+x moasdawiki.sh
   ```
5. Create systemd service
   ```
   sudo cp /path/to/moasdawiki.service /etc/systemd/system
   ```
6. Activate und start systemd service
   ```
   sudo systemctl enable moasdawiki.service
   sudo systemctl start moasdawiki.service
   ```

#### Using SysVinit

1. Install required packages
   - `openjdk-8-jre` (any Java VM 1.8 or higher)
   - `jsvc` (see http://jakarta.apache.org/commons/daemon/)
   - `libcommons-daemon-java`
2. Open the file `moasdawiki.sh` with a text editor and adjust the paths.
3. Mark the file as executable
   ```
   chmod a+x moasdawiki.sh
   ```
4. Create symbolic link in `/etc/init.d`
   ```
   sudo ln -s /path/to/moasdawiki.sh /etc/init.d/moasdawiki
   ```
5. Create symbolic links for daemon start up and shut down in runlevel 2:
   ```
   cd /etc/rc2.d
   sudo ln -s ../init.d/moasdawiki S95moasdawiki
   sudo ln -s ../init.d/moasdawiki K05moasdawiki
   ```

## Usage
### Browser URL

After starting the Wiki server it can be accessed by any web browser opening the URL:
http://localhost:11080/

If the Wiki server runs on a different host, use the host name or its IP address
instead of `localhost`. The default port is 11080. It can be changed in the file `config.txt`.

### User guide

A detailed description of MoasdaWiki server is included in the default Wiki repository.
To open the user guide click on the link "Help" in the navigation panel or open the URL
http://localhost:11080/view/wiki/

## Support

If you have questions or any problems you can contact me via [support@moasdawiki.net]().

## License

MoasdaWiki server is licensed under the GPL 3 license - see the LICENSE file for details.

Copyright (C) Herbert Reiter
