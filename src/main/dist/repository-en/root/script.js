/******************************************
  General helper methods
*******************************************/

/* Create Ajax communication object. */
createRequestObject = function() {
	return new XMLHttpRequest();
};

/* Add a CSS class to a HTML element. */
addCssClass = function(elementOrId, cssClass) {
	var elementNode;
	if (typeof (elementOrId) == 'string') {
		elementNode = document.getElementById(elementOrId);
	} else {
		elementNode = elementOrId;
	}
	if (elementNode && !isCssClassSet(elementOrId, cssClass)) {
		elementNode.className += ' ' + cssClass;
	}
};

/* Remove a CSS class from a HTML element. */
removeCssClass = function(elementOrId, cssClass) {
	var elementNode;
	if (typeof (elementOrId) == 'string') {
		elementNode = document.getElementById(elementOrId);
	} else {
		elementNode = elementOrId;
	}
	if (elementNode) {
		var re = new RegExp('(?:^|\\s)' + cssClass + '(?!\\S)', 'g');
		elementNode.className = elementNode.className.replace(re, '');
	}
};

/* Add or remove a CSS class to/from a HTML element. */
setCssClass = function(elementOrId, cssClass, enable) {
	if (enable) {
		addCssClass(elementOrId, cssClass);
	} else {
		removeCssClass(elementOrId, cssClass);
	}
};

/* Check if a CSS class is present at a HTML element. */
isCssClassSet = function(elementOrId, cssClass) {
	var elementNode;
	if (typeof (elementOrId) == 'string') {
		elementNode = document.getElementById(elementOrId);
	} else {
		elementNode = elementOrId;
	}
	var re = new RegExp('(?:^|\\s)' + cssClass + '(?!\\S)', '');
	return (elementNode && (elementNode.className.match(re) != null));
};

/* Makes a HTML element visible or invisible.
 * show: boolean
 */
setDisplay = function(elementOrId, show) {
	var elementNode;
	if (typeof (elementOrId) == 'string') {
		elementNode = document.getElementById(elementOrId);
	} else {
		elementNode = elementOrId;
	}

	if (show && show == true) {
		elementNode.style.display = 'block';
	} else {
		elementNode.style.display = 'none';
	}
};

/* Inserts text in the text area. */
insertIntoTextarea = function(elementOrId, text) {
	var elementNode;
	if (typeof (elementOrId) == 'string') {
		elementNode = document.getElementById(elementOrId);
	} else {
		elementNode = elementOrId;
	}
	if (typeof document.selection != 'undefined') {
		// Internet Explorer
		var range = document.selection.createRange();
		range.text = text;
	} else if (typeof elementNode.selectionStart != 'undefined') {
		// Gecko based browsers
		var start = elementNode.selectionStart;
		var end = elementNode.selectionEnd;
		elementNode.value = elementNode.value.substr(0, start) + text
				+ elementNode.value.substr(end);
		elementNode.selectionStart = start + text.length;
		elementNode.selectionEnd = start + text.length;
	}
};

/******************************************
  Wiki editor helper methods
*******************************************/

/* Ask for user confirmation before a wiki page is deleted. */
sendDelete = function() {
	if (confirm(deleteConfirmationMsg)) {
		var form = document.forms['EditForm'];
		var el = document.createElement('input');
		el.type = 'hidden';
		el.name = 'delete';
		el.value = 'delete';
		form.appendChild(el);
		form.submit();
	}
};

/* Called if user selects a template in the template dropdown. */
insertTemplate = function(selectedIndex) {
	if (selectedIndex >= 1) {
		var templateContent = templates[selectedIndex - 1].content;
		document.forms["EditForm"].elements["contenteditor"].value += templateContent;
	}
};

/* Determine the inner height of the browser window. */
getWindowHeight = function() {
	if (window.innerHeight) {
		// Firefox and others
		return window.innerHeight;
	} else if (document.documentElement
			&& document.documentElement.clientHeight) {
		// IE in standard-compliant mode
		return document.documentElement.clientHeight;
	} else if (document.body && document.body.clientHeight) {
		// IE in normal mode
		return document.body.clientHeight;
	} else {
		return undefined;
	}
};

/* Adopt the edit area to the browser height. */
resizeEditor = function() {
	var contenteditorTop = document.forms["EditForm"].elements["contenteditor"].offsetTop;
	var controlareaHeight = document.getElementById('controlarea').offsetHeight;
	var height = getWindowHeight() - controlareaHeight - contenteditorTop - 10;
	if (height > 50) {
		document.forms["EditForm"].elements["contenteditor"].style.height = height + "px";
	}
};

getShortPath = function(path) {
	if (path.indexOf(folderPath) == 0) {
		// same folder or subfolder --> relative
		return path.substring(folderPath.length);
	} else {
		// different folder --> keep absolute
		return path;
	}
};

/* Visual effect of "uploadares" during drag&drop. */
handleFileDragOver = function(event) {
	event.stopPropagation();
	event.preventDefault();
	if (event.type == 'dragover') {
		addCssClass('uploadarea', 'dragover');
	} else {
		removeCssClass('uploadarea', 'dragover');
	}
};

/* Called after a file is dropped on the upload area. */
handleFileDrop = function(event) {
	// hover entfernen
	handleFileDragOver(event);

	var files = event.dataTransfer.files;
	if (!files) {
		return;
	}
	if (files.length < 1) {
		alert(uploadNoFileMsg);
		return;
	}
	if (files.length > 1) {
		alert(uploadMultipleFilesMsg);
		return;
	}
	if (files[0].size > 1000000) {
		alert(uploadTooBigMsg);
		return;
	}

	showPanel('uploadPanelId', files[0]);
};

/* Called if the user selects a file in the panel. */
handleFileSelect = function(event) {
	var files = event.target.files;
	if (!files) {
		// nichts tun
	} else if (files.length < 1) {
		alert(uploadNoFileMsg);
		files = null;
	} else if (files.length > 1) {
		alert(uploadMultipleFilesMsg);
		files = null;
	} else if (files[0].size > 1000000) {
		alert(uploadTooBigMsg);
		files = null;
	}

	if (files && files.length > 0) {
		fillUploadInfo(files[0]);
	} else {
		fillUploadInfo();
	}
};

/* Initialize the upload panel and make it visible.
 * panelId: String, panelId DIV
 * selectedFile: File, dropped on upload area, null -> show file select in panel
 */
showPanel = function(panelId, selectedFile) {
	document.uploadForm.reset();

	var showFileSelect = (typeof (selectedFile) == 'undefined');
	setDisplay('fileSelectId', showFileSelect);

	fillUploadInfo(selectedFile);

	setDisplay(panelId, true);
};

/* Hide the upload panel.
 * panelId: String, panelId DIV
 */
hidePanel = function(panelId) {
	setDisplay(panelId, false);
};

fillUploadInfo = function(selectedFile) {
	var selectedFileDefined = (typeof selectedFile != 'undefined');

	// Suggest a repository file name
	var uploadPath = folderPath;
	if (selectedFileDefined) {
		uploadPath += selectedFile.name;
	}
	document.uploadForm.uploadRepositoryPath.value = uploadPath;
	document.uploadForm.uploadRepositoryPath.disabled = !selectedFileDefined;

	// show image tag checkbox only for an image
	var isImage = (selectedFile && selectedFile.name
			.match(/\.(gif|jpg|jpeg|png)$/i) != null);
	document.uploadForm.generateImageTag.checked = isImage;
	document.uploadForm.generateImageTag.disabled = !isImage;
	setCssClass('generateImageTagLabelId', 'disabled',
			!document.uploadForm.generateImageTag.checked);

	// download link
	document.uploadForm.generateFileTag.disabled = !selectedFileDefined;
	document.uploadForm.generateFileTag.checked = selectedFileDefined
			&& !document.uploadForm.generateImageTag.checked;
	setCssClass('generateFileTagLabelId', 'disabled', !selectedFileDefined);

	// upload button
	document.uploadForm.uploadButton.disabled = !selectedFileDefined;

	// remember file
	lastSelectedFile = selectedFile;
};

/* Called if user clicks on upload button. */
handleFileUpload = function(panelId) {
	var uploadPath = document.uploadForm.uploadRepositoryPath.value;
	if (uploadPath.length > 0 && uploadPath.charAt(0) != '/') {
		// make relative path absolute
		uploadPath = folderPath + uploadPath;
	}
	var url = '/upload' + uploadPath;
	var http = createRequestObject();
	if (http.upload) {
		http.open("POST", url, true);
		http.send(lastSelectedFile);
	}
	http.onreadystatechange = function(e) {
		if (http.readyState == 4) {
			// upload finished
			hidePanel(panelId);
			if (http.status == 200) {
				// add tags to wiki page content
				if (document.uploadForm.generateImageTag.checked) {
					var tag = '{{image:' + getShortPath(uploadPath) + '}}\n';
					insertIntoTextarea(document.EditForm.contenteditor, tag);
				}
				if (document.uploadForm.generateFileTag.checked) {
					var tag = '[[file:' + getShortPath(uploadPath) + ']]\n';
					insertIntoTextarea(document.EditForm.contenteditor, tag);
				}
			} else {
				var jsonObj = eval('(' + http.responseText + ')');
				alert('Konnte die Datei nicht hochladen: ' + jsonObj.message);
			}
		}
	};
};

/* Called after web page loading.
 * newPage: Boolean, true -> new wiki page, false -> existing wiki page
 */
initPage = function(newPage) {
	if (newPage) {
		var field = document.forms["EditForm"].elements["titleeditor"];
		field.focus();
		field.selectionStart = field.value.length;
		field.selectionEnd = field.value.length;
	} else {
		var field = document.forms["EditForm"].elements["contenteditor"];
		field.focus();
		field.selectionStart = 0;
		field.selectionEnd = 0;
	}

	// fill template dropdown
	var templateSelectTag = document.forms["EditForm"].elements["TemplateSelect"];
	for (i = 0; i < templates.length; i++) {
		templateSelectTag.options[templateSelectTag.options.length] = new Option(
				templates[i].name);
	}

	// resize editor area
	resizeEditor();
	window.onresize = resizeEditor;

	// attach listeners
	var uploadarea = document.getElementById('uploadarea');
	uploadarea.addEventListener("dragover", handleFileDragOver, false);
	uploadarea.addEventListener("dragleave", handleFileDragOver, false);
	uploadarea.addEventListener("drop", handleFileDrop, false);
	var fileInput = document.getElementById('fileInputId');
	fileInput.addEventListener('change', handleFileSelect, false);
};

/******************************************
  JavaScript support for synchronization
*******************************************/

syncPermitSession = function(sessionId) {
	var url = '/sync-gui/session-permit?session-id=' + sessionId;
	var http = createRequestObject();
	if (http.upload) {
		http.open("GET", url, true);
		http.send();
	}
	http.onreadystatechange = function(e) {
		if (http.readyState == 4) {
			// request finished
			if (http.status == 200) {
				var jsonObj = eval('(' + http.responseText + ')');
				if (jsonObj.code == 0) {
					location.reload();
				} else {
					alert('Error: ' + jsonObj.message);
				}

			} else {
				alert('HTTP Status Code: ' + http.status);
			}
		}
	};
};

syncDropSession = function(sessionId) {
	var url = '/sync-gui/session-drop?session-id=' + sessionId;
	var http = createRequestObject();
	if (http.upload) {
		http.open("GET", url, true);
		http.send();
	}
	http.onreadystatechange = function(e) {
		if (http.readyState == 4) {
			// request finished
			if (http.status == 200) {
				var jsonObj = eval('(' + http.responseText + ')');
				if (jsonObj.code == 0) {
					location.reload();
				} else {
					alert('Error: ' + jsonObj.message);
				}

			} else {
				alert('HTTP Status Code: ' + http.status);
			}
		}
	};
};
