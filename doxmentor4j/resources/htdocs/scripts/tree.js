/*
 * Content-seperated javascript tree widget
 * Copyright (C) 2005 SilverStripe Limited
 * Feel free to use this on your websites, but please leave this message in the files
 * http://www.silverstripe.com/blog
*/

/*
* Ajax-related modifications by Daniel R Somerfield at Outside in
* Copyright (C) 2007 OutsideIn
* http://www.outsidein.org
* 
*/
function TreeDefaults()
{
	url = "";
	this.loadChildren = loadChildren;
	this.getAjaxParams = getAjaxParams;
	this.onSuccess = onSuccess;
	this.onFailure = onFailure;

	function loadChildren (toReplace, paramNode) {
	
		list = document.createElement('ul');
		
		//insert the results of the call into the list
		var url = Tree.url;
		if (url == "")
		{
			alert("You need to set Tree.url to the destination url.");	
		}
		new Ajax.Request(Tree.url, 
		{
			method: 'post', 
			parameters: Tree.getAjaxParams(toReplace, paramNode), 
			onSuccess: Tree.onSuccess,
			onFailure: Tree.onFailure
		});
	};	

	function getAjaxParams (toReplace, paramNode) {
		return 'value=' + paramNode.id;	
	};

	function onSuccess (originalRequest){
		list.innerHTML = originalRequest.responseText;
		toReplace.parentNode.replaceChild(list, toReplace);	
		initTree(list);	
	};

	function onFailure (originalRequest){
		list.innerHTML = "<li>[error]</li>";
		toReplace.parentNode.replaceChild(list, toReplace);
		initTree(list);
		alert("An error occured: " + originalRequest.statusText);
	
	};	
	
}

var Tree = new TreeDefaults();

function resetTreeDefaults() {
	Tree = new TreeDefaults();
}

/*
 * Initialise all trees identified by <ul class="tree">
 */
function autoInit_trees() {
	var candidates = document.getElementsByTagName('ul');
	for(var i=0;i<candidates.length;i++) {
		if(candidates[i].className && candidates[i].className.indexOf('tree') != -1) {
			initTree(candidates[i]);
			candidates[i].className = candidates[i].className.replace(/ ?unformatted ?/, ' ');
		}
	}
}
 
function initTreeForElement(id) {
	var element = document.getElementById(id);
	initTree(element);	
}

/*
 * Initialise a tree node, converting all its LIs appropriately
 */
function initTree(el) {
	var i,j;
	var spanA, spanB, spanC;
	var startingPoint, stoppingPoint, childUL;
	
	// Find all LIs to process
	for(i=0;i<el.childNodes.length;i++) {
		if(el.childNodes[i].tagName && el.childNodes[i].tagName.toLowerCase() == 'li') {
			var li = el.childNodes[i];

			// Create our extra spans
			spanA = document.createElement('span');
			spanB = document.createElement('span');
			spanC = document.createElement('span');
			spanA.appendChild(spanB);
			spanB.appendChild(spanC);
			spanA.className = 'a ' + li.className.replace('closed','spanClosed');
			spanA.onMouseOver = function() {}
			spanB.className = 'b';
			spanB.onclick = treeToggle;
			spanC.className = 'c';
			
			
			// Find the UL within the LI, if it exists
			stoppingPoint = li.childNodes.length;
			startingPoint = 0;
			childUL = null;
			for(j=0;j<li.childNodes.length;j++) {
				if(li.childNodes[j].tagName && li.childNodes[j].tagName.toLowerCase() == 'div') {
					startingPoint = j + 1;
					continue;
				}

				if(li.childNodes[j].tagName && li.childNodes[j].tagName.toLowerCase() == 'ul') {
					childUL = li.childNodes[j];
					stoppingPoint = j;
					break;					
				}
			}
				
			// Move all the nodes up until that point into spanC
			for(j=startingPoint;j<stoppingPoint;j++) {
				spanC.appendChild(li.childNodes[startingPoint]);
			}
			
			// Insert the outermost extra span into the tree
			if(li.childNodes.length > startingPoint) li.insertBefore(spanA, li.childNodes[startingPoint]);
			else li.appendChild(spanA);
			
			// Process the children
			if(childUL != null) {
				if(initTree(childUL)) {
					addClass(li, 'children', 'closed');
					addClass(spanA, 'children', 'spanClosed');
				}
			}
		}
	}
	
	if(li) {
		// li and spanA will still be set to the last item

		addClass(li, 'last', 'closed');
		addClass(spanA, 'last', 'spanClosed');
		return true;
	} else {
		return false;
	}	
}
 

/*
 * +/- toggle the tree, where el is the <span class="b"> node
 * force, will force it to "open" or "close"
 */
function treeToggle(el, force) {
	el = this;
	
	while(el != null && (!el.tagName || el.tagName.toLowerCase() != "li")) el = el.parentNode;
	
	// Get UL within the LI
	var childSet = findChildWithTag(el, 'ul');
	var topSpan = findChildWithTag(el, 'span');

	if( force != null ){
		
		if( force == "open"){
			treeOpen( topSpan, el )
		}
		else if( force == "close" ){
			treeClose( topSpan, el )
		}
		
	}
	
	else if( childSet != null) {
		// Is open, close it
		if(!el.className.match(/(^| )closed($| )/)) {		
			treeClose( topSpan, el )
		// Is closed, open it
		} else {			
			treeOpen( topSpan, el )
		}
	}
}

function loadChildren(a) {

	parentLI = a.parentNode;
	toReplace = findChildWithTag(parentLI, "ul");
	paramNode = findChildWithTag(toReplace, "li");
	replaceWithLoad(toReplace, paramNode)

}

function scrollPage (a) {
	parentLI = a.parentNode;
	paramUL = findChildWithTag(parentLI, "ul");
	paramNode = findChildWithTag(paramUL, "li");
	
	toReplace = parentLI.parentNode
	replaceWithLoad(toReplace, paramNode)
	
}

function replaceWithLoad(toReplace, paramNode){
	Tree.loadChildren(toReplace, paramNode);
}

function treeOpen( a, b ){
	if(a.className.match(/(^| )unloaded($| )/)) {		
		loadChildren(a);
		removeClass(a,'unloaded')
	}
	if(a.className.match(/(^| )next_page($| )/)) {		
		scrollPage(a);
		removeClass(a,'next_page')
	}
	if(a.className.match(/(^| )previous_page($| )/)) {		
		scrollPage(a);
		removeClass(a,'previous_page')
	}
	removeClass(a,'spanClosed');
	removeClass(b,'closed');
}
	
	
function treeClose( a, b ){
	addClass(a,'spanClosed');
	addClass(b,'closed');
}

/*
 * Find the a child of el of type tag
 */
function findChildWithTag(el, tag) {
	for(var i=0;i<el.childNodes.length;i++) {
		if(el.childNodes[i].tagName != null && el.childNodes[i].tagName.toLowerCase() == tag) return el.childNodes[i];
	}
	return null;
}

/*
 * Functions to add and remove class names
 * Mac IE hates unnecessary spaces
 */
function addClass(el, cls, forceBefore) {
	if(forceBefore != null && el.className.match(new RegExp('(^| )' + forceBefore))) {
		el.className = el.className.replace(new RegExp("( |^)" + forceBefore), '$1' + cls + ' ' + forceBefore);

	} else if(!el.className.match(new RegExp('(^| )' + cls + '($| )'))) {
		el.className += ' ' + cls;
		el.className = el.className.replace(/(^ +)|( +$)/g, '');
	}
}
function removeClass(el, cls) {
	var old = el.className;
	var newCls = ' ' + el.className + ' ';
	newCls = newCls.replace(new RegExp(' (' + cls + ' +)+','g'), ' ');
	el.className = newCls.replace(/(^ +)|( +$)/g, '');
} 

/*
 * Handlers for automated loading
 */ 
 _LOADERS = Array();

function callAllLoaders() {
	var i, loaderFunc;
	for(i=0;i<_LOADERS.length;i++) {
		loaderFunc = _LOADERS[i];
		if(loaderFunc != callAllLoaders) loaderFunc();
	}
}

function appendLoader(loaderFunc) {
	if(window.onload && window.onload != callAllLoaders)
		_LOADERS[_LOADERS.length] = window.onload;

	window.onload = callAllLoaders;

	_LOADERS[_LOADERS.length] = loaderFunc;
}

appendLoader(autoInit_trees);
