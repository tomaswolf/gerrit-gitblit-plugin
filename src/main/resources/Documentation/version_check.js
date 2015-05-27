// Copyright (C) 2015 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
(function() {
	
	var lk = document.getElementById("gerrit-gitblit-version-check");
	var em = document.getElementById("gerrit-gitblit-current-version");
	
	if (!lk || !em) {
		lk = em = null;
		return;
	}

	function addHandler(element, evt, handler) {
		if (element.addEventListener) {
			element.addEventListener (evt, handler, false);
		} else if (element.attachEvent) {
			element.attachEvent ('on' + evt, handler);
		} else {
			element['on' + evt] = handler;
		}
	}
	
	function split(tag) {
		if (tag.charAt(0) == 'v') tag = tag.substring(1);
		var i = tag.lastIndexOf('.');
		var result = {};
		result.plugin = tag.substring(i+1);
		tag = tag.substring(0, i);
		i = tag.lastIndexOf('.');
		result.gitblit = tag.substring(i+1);
		result.gerrit = tag.substring(0, i);
		return result;
	}
	
	function killEvt (e) {
		e = e || window.event || window.Event;
		if (typeof e.preventDefault != 'undefined') {
			e.preventDefault ();
			e.stopPropagation ();
		} else {
			e.cancelBubble = true;
		}
		return false;
	}

	function clickHandler(evt) {
		window.pluginVersionCheck = versionCheck;
		var head = document.getElementsByTagName ('head')[0];
		var s = document.createElement ('script');
		s.setAttribute ('src', 'https://api.github.com/repos/tomaswolf/gerrit-gitblit-plugin/releases?callback=pluginVersionCheck');
		s.setAttribute ('type', 'text/javascript');
		head.insertBefore (s, head.firstChild);
		return killEvt(evt);
	}
	
	var currentTag = split(em.innerHTML);
	em = null;
	
	function versionCheck(json) {
		if (lk === null) return;
		var parent = lk.parentNode;
		if (json && json.meta && json.meta.status == 200) {
			if (json.data && json.data.length > 0) {
				// An array of objects.
				for (var k=0; k < json.data.length; k++) {
					if (!json.data[k].draft && !json.data[k].prerelease) {
						var tag = json.data[k].tag_name;
						var splitTag = split(tag);
						if (splitTag.gerrit == currentTag.gerrit && (splitTag.gitblit > currentTag.gitblit || splitTag.gitblit == currentTag.gitblit && splitTag.plugin > currentTag.plugin)) {
							var newLk = document.createElement('a');
							newLk.href = json.data[k].html_url;
							newLk.appendChild(document.createTextNode("A newer version " + tag + " is available."));
							parent.insertBefore(newLk, lk);
							parent.removeChild(lk);
							delete window.pluginVersionCheck;
							lk = null;
							return;
						}
					}
				}
			}
			parent.insertBefore(document.createTextNode("This is the current plugin version for this Gerrit version."), lk);
		} else {
			parent.insertBefore(document.createTextNode("Could not retrieve update information from GitHub."), lk);
		}
		parent.removeChild(lk);
		delete window.pluginVersionCheck;
		lk = null;
	}
	
	lk.style.display = '';
	addHandler(lk, 'click', clickHandler);
})();