# MoasdaWiki Server

## Description

MoasdaWiki Server is a privacy-friendly and interactive knowledge management
tool. It provides a browser-based GUI to search, create, and modify content in
Wiki style.

For documentation see https://moasdawiki.net/.

### Key features

- Easy usability
- No installation required, can be unzipped and run on a USB stick
- No special system requirements like database or application server
- Runs on any system with a Java VM 1.8 or higher
- [MoasdaWiki App](https://gitlab.com/moasdawiki/moasdawiki-app) available,
  always have a copy of the Wiki content on your mobile device
- Data privacy by design: Stores content only on the local computer, never
  establishes a cloud connection
- Free/Libre and Open Source Software (FLOSS)

### Feature list

- Powerful syntax, supports tables and images
- Fast full-text search
- Page templates to create uniform looking pages
- Full customizable graphical user interface
  - Navigation panel can be modified
  - Page layout for view in browser and for printing is defined by CSS stylesheet
  - Page header and footer can be modified
  - Customizable HTML header, e.g. to include your own CSS stylesheets and JavaScript libraries
  - Configuration file with several options
- Internationalization via message files
- List of recently modified and viewed pages
- Built-in lightweight web server, no external web server required
- Stores pages as text files in a folder in the local file system;
  for backups just make a copy of that folder.

## Download / Build

**Download** MoasdaWiki server from the
[releases page](https://gitlab.com/moasdawiki/moasdawiki-server/-/releases).
Unzip the file `moasdawiki-server-x.y.z.zip` in any folder.
No special installation steps necessary.

Alternatively, you can clone the GitLab repository and **build** the project locally:

```
gradle build
```

Afterwards, you will find the target ZIP file in the folder ```build/distributions/```.

## Run from Terminal (Linux/Windows)

MoasdaWiki server requires Java 1.8 or higher and is running on several
operating systems and platforms.

1. Open a Terminal
2. Go to folder that contains `moasdawiki-server-x.y.z.jar`
3. `java -jar moasdawiki-server-x.y.z.jar repository-en`

After starting the Wiki server it can be accessed by any web browser, open the URL:
http://localhost:11080/

If the Wiki server runs on a different host, use the host name or its IP address
instead of `localhost`. The default port is 11080. It can be changed in the file
`repository/config.txt`.

## Run as a Linux daemon
### Using systemd (since Ubuntu 15.10)

1. Check if systemd is running
   ```
   $ cat /proc/1/comm
   systemd
   ```

2. Install required packages
   - `openjdk-8-jre` (any Java VM 1.8 or higher)
   - `jsvc` (see https://commons.apache.org/proper/commons-daemon/)
   - `libcommons-daemon-java`

3. Edit the files `moasdawiki.sh` and `moasdawiki.service` and adjust the paths.

4. Mark the files as executable
   ```
   chmod a+x moasdawiki.service
   chmod a+x moasdawiki.sh
   ```

5. Create a systemd service
   ```
   sudo cp /path/to/moasdawiki.service /etc/systemd/system
   ```

6. Activate und start the systemd service
   ```
   sudo systemctl enable moasdawiki.service
   sudo systemctl start moasdawiki.service
   ```

### Using SysVinit

1. Install required packages
   - `openjdk-8-jre` (any Java VM 1.8 or higher)
   - `jsvc` (see https://commons.apache.org/proper/commons-daemon/)
   - `libcommons-daemon-java`

2. Edit the file `moasdawiki.sh` and adjust the paths.

3. Mark the file as executable
   ```
   chmod a+x moasdawiki.sh
   ```

4. Create a symbolic link in `/etc/init.d`
   ```
   sudo ln -s /path/to/moasdawiki.sh /etc/init.d/moasdawiki
   ```

5. Create symbolic links for daemon start up and daemon shut down in runlevel 2:
   ```
   cd /etc/rc2.d
   sudo ln -s ../init.d/moasdawiki S95moasdawiki
   sudo ln -s ../init.d/moasdawiki K05moasdawiki
   ```

## User guide

A detailed description of the MoasdaWiki server is bundled in the downloaded ZIP file.
To open the user guide click on the "Help" link in the navigation panel or open the URL
http://localhost:11080/view/wiki/

## Support

If you have questions or any problems you can contact me via [support@moasdawiki.net]().

## License

MoasdaWiki server is licensed under the GPL 3 license &ndash; see the
[LICENSE](https://gitlab.com/moasdawiki/moasdawiki-server/-/blob/master/LICENSE)
file for details.

Copyright (C) Herbert Reiter
