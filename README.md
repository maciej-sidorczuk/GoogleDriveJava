This program which works from linux cli and windows cmd allows you to make backups to google drive. You can use it for example in raspberryPi in cron to make regular backups.
Except full backups program allows you to make backups only for files which changed or not exist in backup.

Instructions:

After you clone project folder from git repo to eclipse's workspace:
1. Install gradle in your system if you don't have gradle installed.
2. Get client_secret.json from your Goole API Account
	a. Use this wizard to create or select a project in the Google Developers Console and automatically turn on the API. Click Continue, then Go to credentials.
	b. On the Add credentials to your project page, click the Cancel button.
	c. At the top of the page, select the OAuth consent screen tab. Select an Email address, enter a Product name if not already set, and click the Save button.
	d. Select the Credentials tab, click the Create credentials button and select OAuth client ID.
	e. Select the application type Other, enter the name "Drive API Quickstart", and click the Create button.
	f. Click OK to dismiss the resulting dialog.
	g. Click the file_download (Download JSON) button to the right of the client ID.
	h. Move this file to your working directory and rename it client_secret.json.
3. Run eclipse. Click File and then Open Projects from File System...
4. Click Directory... and find your project folder. Select project and click Finish button.
5. Find imported project on the list. Click right on project and go to: Configure -> Add Gradle Nature
6. Exit eclipse. Copy client_secret.json from step 1 to bin folder.
7. Run eclipse. Find the project and navigate to Quickstart.java file. Click right on this file and select Run as "Java Application". If you use Run by default it will run Gradle Test instead of java program.

Program needs parameters.

First parameter is id of folder located in google drive. We will send backups to this folder. You can get ID of this folder by going to google drive using your web browser. Go to this folder and look at the url. The last string in the url is the id of folder.

Second parameter is path to your local folder on your system which you will make a backup.

Third parameter is type of operation:

generatechecksums - it generates checksums of files located in your folder which you will make backup. Checksums are used when you will make partial backup (backup only files which changed or not exist in backup). Program compares checksums of local files and files in google drive and based on results decides whether override google drive files or not. This mode will not make a backup.

synchronize - in this mode program compare google drive folder and local folder using checksums. When checksums are not equals then local file overrides file in google drive.

clean - it cleans file with checksums. Once for a while it is recommend to use this mode. This mode will not make any backup.
If you don't provide a third argument program will make a normal backup.
