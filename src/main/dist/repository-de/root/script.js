/*
 * Allgemeine Hilfsfunktionen.
 */

createRequestObject = function() {
	try {
		return new XMLHttpRequest(); // Mozilla, Opera 8+, Safari, IE 7+
	} catch (e) {
		// Internet Explorer
		try {
			return new ActiveXObject("Microsoft.XMLHTTP"); // IE 6
		} catch (e) {
			try {
				return new ActiveXObject("Msxml2.XMLHTTP"); // IE 5
			} catch (e) {
				return false;
			}
		}
	}
};

/**
 * Fügt eine CSS-Klasse zum Element hinzu. Wenn die CSS-Klasse bereits gesetzt
 * ist, passiert nichts.
 */
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

/**
 * Entfernt einen CSS-Klasse von einem Element. Ist keine solche CSS-Klasse
 * gesetzt, passiert nichts.
 */
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

/**
 * Setzt oder entfernt die CSS-Klasse bei einem Element abhängig vom parameter
 * "enable".
 */
setCssClass = function(elementOrId, cssClass, enable) {
	if (enable) {
		addCssClass(elementOrId, cssClass);
	} else {
		removeCssClass(elementOrId, cssClass);
	}
};

/**
 * Gibt zurück, ob eine bestimmte CSS-Klasse bei einem Element gesetzt ist.
 */
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

/**
 * Macht ein Element sichtbar oder unsichtbar.
 * 
 * @param (boolean)
 *            show <code>true</code> --> sichtbar machen, ansonsten unsichtbar
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

/**
 * Fügt Text in das angegebene Textfeld ein.
 */
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
		// neuere auf Gecko basierende Browser
		var start = elementNode.selectionStart;
		var end = elementNode.selectionEnd;
		elementNode.value = elementNode.value.substr(0, start) + text
				+ elementNode.value.substr(end);
		elementNode.selectionStart = start + text.length;
		elementNode.selectionEnd = start + text.length;
	}
};

/*
 * JavaScript-Unterstützung für den Wiki-Editor.
 * 
 * Einige Scripte setzen voraus, dass eine Variable "templates" mit der Liste
 * aller Wikivorlagen definiert ist. Beispiel:
 * 
 * var templates = [{name: "Name", content: "Inhalt"}];
 */

/**
 * Löscht die aktuelle Wikiseite. Wird aufgerufen, wenn der Benutzer auf den
 * Löschen-Button klickt.
 */
sendDelete = function() {
	if (confirm('Wikiseite wirklich löschen?')) {
		var form = document.forms['EditForm'];
		var el = document.createElement('input');
		el.type = 'hidden';
		el.name = 'delete';
		el.value = 'delete';
		form.appendChild(el);
		form.submit();
	}
};

/**
 * Wird vom onchange-Event des Template-Dropdowns aufgerufen, wenn der Benutzer
 * einen Eintrag auswählt.
 * 
 * @param {Integer}
 *            selectedIndex
 */
insertTemplate = function(selectedIndex) {
	if (selectedIndex >= 1) {
		var templateContent = templates[selectedIndex - 1].content;
		document.forms["EditForm"].elements["contenteditor"].value += templateContent;
	}
};

/**
 * Ermittelt browserübergreifend die aktuelle Innenhöhe des Browserfensters.
 * 
 * @see http://de.selfhtml.org/javascript/objekte/window.htm#inner_height
 */
getWindowHeight = function() {
	if (window.innerHeight) {
		// Firefox und andere Browser
		return window.innerHeight;
	} else if (document.documentElement
			&& document.documentElement.clientHeight) {
		// IE im standardkonformen Modus
		return document.documentElement.clientHeight;
	} else if (document.body && document.body.clientHeight) {
		// IE im Normalmodus
		return document.body.clientHeight;
	} else {
		return undefined;
	}
};

/**
 * Passt die Höhe des mehrzeiligen Editors an die Höhe des Browserfensters an.
 */
resizeEditor = function() {
	var contenteditorTop = document.forms["EditForm"].elements["contenteditor"].offsetTop;
	var controlareaHeight = document.getElementById('controlarea').offsetHeight;
	var height = getWindowHeight() - controlareaHeight - contenteditorTop - 10;
	if (height > 50) {
		document.forms["EditForm"].elements["contenteditor"].style.height = height
				+ "px";
	}
};

getShortPath = function(path) {
	if (path.indexOf(folderPath) == 0) {
		// selber oder Unterordner --> relativ
		return path.substring(folderPath.length);
	} else {
		// anderer Pfad --> absolut lassen
		return path;
	}
};

/**
 * Steuert den visuellen Effekt der "uploadarea" bei Drag&Drop-Vorgang.
 */
handleFileDragOver = function(event) {
	event.stopPropagation();
	event.preventDefault();
	if (event.type == 'dragover') {
		addCssClass('uploadarea', 'dragover');
	} else {
		removeCssClass('uploadarea', 'dragover');
	}
};

/**
 * Wird aufgerufen, wenn eine Datei per Drag&Drop abgelegt wird. Öffnet das
 * Upload-Panel.
 */
handleFileDrop = function(event) {
	// hover entfernen
	handleFileDragOver(event);

	var files = event.dataTransfer.files;
	if (!files) {
		return;
	}
	if (files.length < 1) {
		alert('Keine Datei ausgewählt!');
		return;
	}
	if (files.length > 1) {
		alert('Bitte nur eine Datei auswählen!');
		return;
	}
	if (files[0].size > 1000000) {
		alert('Datei darf nicht größer als 1 MB sein!');
		return;
	}

	showPanel('uploadPanelId', files[0]);
};

/**
 * Wird aufgerufen, wenn im Panel eine Datei ausgewählt wird.
 */
handleFileSelect = function(event) {
	var files = event.target.files;
	if (!files) {
		// nichts tun
	} else if (files.length < 1) {
		alert('Keine Datei ausgewählt!');
		files = null;
	} else if (files.length > 1) {
		alert('Bitte nur eine Datei auswählen!');
		files = null;
	} else if (files[0].size > 1000000) {
		alert('Datei darf nicht größer als 1 MB sein!');
		files = null;
	}

	if (files && files.length > 0) {
		fillUploadInfo(files[0]);
	} else {
		fillUploadInfo();
	}
};

/**
 * Initialisiert die Panelfelder und zeigt das Panel an.
 * 
 * @param (String)
 *            panelId DIV, das das Panel enthält. Wird sichtbar gemacht.
 * @param (File)
 *            selectedFile Wenn gesetzt, wurde per Drag&Drop bereits eine Datei
 *            ausgewählt. Dann wird die Dateiauswahl im Panel unterdrückt.
 */
showPanel = function(panelId, selectedFile) {
	// Formularfelder leeren
	document.uploadForm.reset();

	// Dateiauswahl ggf. unsichtbar machen
	var showFileSelect = (typeof (selectedFile) == 'undefined');
	setDisplay('fileSelectId', showFileSelect);

	// weitere Felder initialisieren
	fillUploadInfo(selectedFile);

	// Panel anzeigen
	setDisplay(panelId, true);
};

/**
 * Macht das Panel unsichtbar.
 * 
 * @param (String)
 *            panelId DIV, das das Panel enthält. Wird unsichtbar gemacht.
 */
hidePanel = function(panelId) {
	// Panel verbergen
	setDisplay(panelId, false);
};

fillUploadInfo = function(selectedFile) {
	var selectedFileDefined = (typeof selectedFile != 'undefined');

	// Repository-Dateiname vorschlagen
	var uploadPath = folderPath;
	if (selectedFileDefined) {
		uploadPath += selectedFile.name;
	}
	document.uploadForm.uploadRepositoryPath.value = uploadPath;
	document.uploadForm.uploadRepositoryPath.disabled = !selectedFileDefined;

	// Bild-Tag-Checkbox nur bei einem Bild anbieten
	var isImage = (selectedFile && selectedFile.name
			.match(/\.(gif|jpg|jpeg|png)$/i) != null);
	document.uploadForm.generateImageTag.checked = isImage;
	document.uploadForm.generateImageTag.disabled = !isImage;
	setCssClass('generateImageTagLabelId', 'disabled',
			!document.uploadForm.generateImageTag.checked);

	// Download-Link
	document.uploadForm.generateFileTag.disabled = !selectedFileDefined;
	document.uploadForm.generateFileTag.checked = selectedFileDefined
			&& !document.uploadForm.generateImageTag.checked;
	setCssClass('generateFileTagLabelId', 'disabled', !selectedFileDefined);

	// Upload-Button
	document.uploadForm.uploadButton.disabled = !selectedFileDefined;

	// Datei merken
	lastSelectedFile = selectedFile;
};

/**
 * Wird vom Upload-Button aufgerufen.
 */
handleFileUpload = function(panelId) {
	// Datei hochladen
	var uploadPath = document.uploadForm.uploadRepositoryPath.value;
	if (uploadPath.length > 0 && uploadPath.charAt(0) != '/') {
		// relativen Pfad absolut machen
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
			// Ende des Uploads
			hidePanel(panelId);
			if (http.status == 200) {
				// Tags in Wikitext einfügen
				if (document.uploadForm.generateImageTag.checked) {
					var tag = '{{image:' + getShortPath(uploadPath) + '}}\n';
					insertIntoTextarea(document.EditForm.contenteditor, tag);
				}
				if (document.uploadForm.generateFileTag.checked) {
					var tag = '[[file:' + getShortPath(uploadPath) + ']]\n';
					insertIntoTextarea(document.EditForm.contenteditor, tag);
				}
			} else {
				// Fehler anzeigen
				var jsonObj = eval('(' + http.responseText + ')');
				alert('Konnte die Datei nicht hochladen: ' + jsonObj.message);
			}
		}
	};
};

/**
 * Wird vom onload-Event der Editorseite aufgerufen, um die Seitenelemente zu
 * initialisieren.
 * 
 * @param {Boolean}
 *            newPage true -> neue Wikiseite wird erstellt,<br>
 *            false -> eine bestehende Wikiseite wird bearbeitet.
 */
initPage = function(newPage) {
	// Fokus in Editor setzen
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

	// Template-Dropdown befüllen
	var templateSelectTag = document.forms["EditForm"].elements["TemplateSelect"];
	for (i = 0; i < templates.length; i++) {
		templateSelectTag.options[templateSelectTag.options.length] = new Option(
				templates[i].name);
	}

	// Editorhöhe anpassen
	resizeEditor();
	window.onresize = resizeEditor;

	// Events einhängen
	var uploadarea = document.getElementById('uploadarea');
	uploadarea.addEventListener("dragover", handleFileDragOver, false);
	uploadarea.addEventListener("dragleave", handleFileDragOver, false);
	uploadarea.addEventListener("drop", handleFileDrop, false);
	var fileInput = document.getElementById('fileInputId');
	fileInput.addEventListener('change', handleFileSelect, false);
};

/*
 * JavaScript-Unterstützung für das SynchronizationPlugin.
 */

syncPermitSession = function(sessionId) {
	var url = '/sync-gui/session-permit?session-id=' + sessionId;
	var http = createRequestObject();
	if (http.upload) {
		http.open("GET", url, true);
		http.send();
	}
	http.onreadystatechange = function(e) {
		if (http.readyState == 4) {
			// Ende des Requests, Antwort ist da
			if (http.status == 200) {
				var jsonObj = eval('(' + http.responseText + ')');
				if (jsonObj.code == 0) {
					location.reload();
				} else {
					alert('Fehler: ' + jsonObj.message);
				}

			} else {
				// Fehler anzeigen
				alert('HTTP-Status-Code: ' + http.status);
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
			// Ende des Requests, Antwort ist da
			if (http.status == 200) {
				var jsonObj = eval('(' + http.responseText + ')');
				if (jsonObj.code == 0) {
					location.reload();
				} else {
					alert('Fehler: ' + jsonObj.message);
				}

			} else {
				// Fehler anzeigen
				alert('HTTP-Status-Code: ' + http.status);
			}
		}
	};
};
