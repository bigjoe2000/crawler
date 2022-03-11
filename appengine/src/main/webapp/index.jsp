<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.bigjoe.crawler.appengine.servlet.HelloAppEngine" %>
<html>
<head>
  <link href='//fonts.googleapis.com/css?family=Marmelad' rel='stylesheet' type='text/css'>
  <title>Appengine crawler</title>
</head>
<body>
    <h1>Crawler</h1>

	<label>Url to crawl:</label><input class="url-entry" type="text">
	<label>Level</label><input type="number" class="level" value="3">
	<label>Max Pages</label><input type="number" class="maxPages" value="300">
	<button class="submit-job">Crawl</button>
	<button class="list-jobs">List Jobs</button>

	<pre class="results"></pre>

<script>

let resultsDiv = document.querySelector('pre.results');
let inputEl = document.querySelector('input.url-entry');
let levelEl = document.querySelector('input.level');
let maxPagesEl = document.querySelector('input.maxPages');

document.querySelector('button.list-jobs').addEventListener('click', function() {
	makeApiCall('/job/list', 'GET', function(resp) {
		resultsDiv.innerText = '';
		for (const i in resp) {
			let el = document.createElement('pre');
			resultsDiv.appendChild(el);
			el.innerText = JSON.stringify(resp[i]);
			let button = document.createElement('button');
			button.innerText = 'Show Job Images';
			let deleteButton = document.createElement('button');
			deleteButton.innerText = 'Delete Job';
			resultsDiv.appendChild(button);
			resultsDiv.appendChild(deleteButton);
			button.addEventListener('click', function() {
				makeApiCall('/job/get?url=' + encodeURIComponent(resp[i].url), 'GET', function(resp) {
					el.innerText = JSON.stringify(resp);
					button.parentElement.removeChild(button);
				});
			});
			deleteButton.addEventListener('click', function() {
				makeApiCall('/job/delete?url=' + encodeURIComponent(resp[i].url), 'POST', function() {
					if (button) {
						button.parentElement.removeChild(button);
					}
					deleteButton.parentElement.removeChild(deleteButton);
					el.parentElement.removeChild(el);
				});
			});
		}
		if (!resp.length) {
			resultsDiv.innerText = 'No jobs found';
		}
	});
})

document.querySelector('button.submit-job').addEventListener('click', function() {
	makeApiCall('/job/create?url=' + encodeURIComponent(inputEl.value) 
			+ '&level=' + levelEl.value
			+ '&maxPages=' + maxPagesEl.value, 'POST', function(resp) {
		resultsDiv.innerText = JSON.stringify(resp);
		inputEl.value = '';
	})
})

makeApiCall = function(url, method, callback) {
	let xhr = new XMLHttpRequest();
	xhr.open(method, url);
	xhr.onreadystatechange = function() {
		if (xhr.readyState == XMLHttpRequest.DONE) {
			if (xhr.status != 200) {
				alert('Server returned:' + xhr.status  + ':' + xhr.responseText);
				return;
			}
			let response = xhr.responseText ? JSON.parse(xhr.responseText) : null;
			callback(response);
		}
	}
	xhr.send();
}
</script>
</body>
</html>
