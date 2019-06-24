var nodes;
var zTNodes;
var writeFilePath = "";
var tcDirPath = "";
var tcFilePath = "";
var zTree, rMenu;
var jenkinsRoot;
var configPYFile ="";
var configfileOpened = false;
var tcfileOpened = false;

var setting = {
	check : {
		enable : true
	},
	callback : {
		onCheck : writeCheckedNodeToFile,
		onRightClick: OnRightClick,
		onClick: openTCFile,
		beforeRename: zTreeBeforeRename
	}
};

function loadTreeFromPath(filePath, argfilePath, conffilePath, rootUrl) {
	if (filePath == null) {
		return;
	}
	if (argfilePath == null) {
		return;
	}
	writeFilePath = argfilePath;
	tcDirPath = filePath;
	configPYFile = conffilePath;
	jenkinsRoot = rootUrl;
	zheZhaoDIV(true);
	TestCaseSelectorRPC.loadRobotTC(filePath, argfilePath, function(t) {
		var res = t.responseObject();
		if (!res) {
			alert('Internal server error');
			return;
		}
		if (!res.success) {
			alert(res.errorMsg);
			return;
		}
		InitTreeFromJson(eval("[" + res.content.replace(/rootURL/g, rootUrl) + "]"));
	});
}

function InitTreeFromJson(zNodes) {
	zTNodes = zNodes;
	jQuery.fn.zTree.init(jQuery("#treeRF"), setting, zNodes);
	zheZhaoDIV(false);
	zTree = jQuery.fn.zTree.getZTreeObj("treeRF");
	rMenu = jQuery("#rMenu");
	count();
}

function expandAll_Tree(expandFlag) {
	var treeObj = jQuery.fn.zTree.getZTreeObj("treeRF");
	treeObj.expandAll(expandFlag);
}

var cArray = new Array();

function writeCheckedNodeToFile() {
	var arrayObj = new Array();
	var treeObj = jQuery.fn.zTree.getZTreeObj("treeRF");
	nodes = treeObj.getCheckedNodes(true);
	for (var i = 0; i < nodes.length; i++) {
		if (nodes[i].isCase) {
			cArray = [];
			traversalPN(nodes[i]);
			arrayObj.push("\n--test\n" + cArray.reverse().join("."));
		}
	}
	count();
	TestCaseSelectorRPC.writeTCConfigFile(writeFilePath, arrayObj.toString(),
			function(t) {
				var res = t.responseObject();
				if (!res) {
					alert('Internal server error');
					return;
				}
				if (!res.success) {
					alert(res.errorMsg);
					return;
				}
			});
}

function count() {
	var checkCount = 0;
	var treeObj = jQuery.fn.zTree.getZTreeObj("treeRF");
	nodes = treeObj.getCheckedNodes(true);
	for (var i = 0; i < nodes.length; i++) {
		if (nodes[i].isCase) {
			checkCount++;
		}
	}
	jQuery("#checkCount").text(checkCount);
}

function traversalPN(node) {
	if (node != null) {
		cArray.push(node.name);
		node = node.getParentNode();
		traversalPN(node);
	}
}

function saveDailyTests() {
    if(confirm("Save daily testcases?")){
		TestCaseSelectorRPC.copyFile(true);
	}
}

function loadDailyTests() {
    if(confirm("Load daily testcases?")){
		TestCaseSelectorRPC.copyFile(false, function(t) {
		var res = t.responseObject();
		if(res == true){loadTreeFromPath(tcDirPath, writeFilePath, configPYFile, jenkinsRoot);}
		});
	}
}

function openTCFile() {
	var treeObj = jQuery.fn.zTree.getZTreeObj("treeRF");
	nodes = treeObj.getSelectedNodes();
	if (nodes[0].isFile) {
		var dir = tcDirPath.substring(tcDirPath.lastIndexOf("\\")+1);
		var subStr = nodes[0].parent.replace(dir, "");
		var retStr = tcDirPath + subStr.replace(/\./g, "\\") + ".txt";
		tcFilePath = retStr;
	}
	else {
		tcFilePath = "";
		return;
	}
	tcfileOpened = true;
	TestCaseSelectorRPC.readFile(tcFilePath, function(t) {
		var res = t.responseObject();
		displayFile(res);
		});
}

function openConfFile() {
	if (configPYFile == "") {
		return;
	}
	configfileOpened = true;
	TestCaseSelectorRPC.readFile(configPYFile, function(t) {
		var res = t.responseObject();
		displayFile(res);
		});
}


function displayFile(content) {
	jQuery("#txtFile").val(content);
}

function saveTCFile() {
	var strFile = jQuery("#txtFile").val();
	if (tcFilePath == "") {
		return;
	}
	if (tcfileOpened == false)
	{
		alert('Testcase file is not opened!');
		return;
	}
	if(confirm("Save TestCase File?" + "\n" + tcFilePath)){
		tcfileOpened = false;
		TestCaseSelectorRPC.writeFile(tcFilePath, strFile, 
		function(t) {
			var res = t.responseObject();
			if (res.success) {
				loadTreeFromPath(tcDirPath, writeFilePath, configPYFile, jenkinsRoot);
				return;
			}
			if (!res) {
				alert('Internal server error');
				return;
			}
			if (!res.success) {
				alert(res.errorMsg);
				return;
			}
		});
	}
}

function saveConfFile() {
	var strFile = jQuery("#txtFile").val();
	if (configPYFile == "") {
		return;
	}
	if (configfileOpened == false)
	{
		alert('Config file is not opened!');
		return;
	}
	if(confirm("Save Config File?" + "\n" + configPYFile)){
		configfileOpened = false;
		zheZhaoDIV(true);
		TestCaseSelectorRPC.writeFile(configPYFile, strFile, 
		function(t) {
			var res = t.responseObject();
			if (res.success) {
				zheZhaoDIV(false);
				return;
			}
			if (!res) {
				alert('Internal server error');
				return;
			}
			if (!res.success) {
				alert(res.errorMsg);
				return;
			}
		});
	}
}

function OnRightClick(event, treeId, treeNode) {
	var st = jQuery("#treeRF").parent().attr('scrollTop');
	var doc = jQuery(document).scrollTop();
	if (!treeNode && event.target.tagName.toLowerCase() != "button" && jQuery(event.target).parents("a").length == 0) {
		zTree.cancelSelectedNode();
		showRMenu("root", event.clientX, event.clientY + st + doc, treeNode.isFile);
	} else if (treeNode && !treeNode.noR) {
		zTree.selectNode(treeNode);
		showRMenu("node", event.clientX, event.clientY + st + doc, treeNode.isFile);
	}
}

function showRMenu(type, x, y, isfile) {
	jQuery("#rMenu ul").show();
	if (type=="root") {
		jQuery("#m_del").hide();
	} else {
		jQuery("#m_del").show();
	}
	if (isfile) {
		jQuery("#m_add").hide();
	} else {
		jQuery("#m_add").show();
	}
	rMenu.css({"top":y+"px", "left":x+"px", "visibility":"visible"});

	jQuery("body").bind("mousedown", onBodyMouseDown);
}

function hideRMenu() {
	if (rMenu) rMenu.css({"visibility": "hidden"});
	jQuery("body").unbind("mousedown", onBodyMouseDown);
}

function onBodyMouseDown(event){
	if (!(event.target.id == "rMenu" || jQuery(event.target).parents("#rMenu").length>0)) {
		rMenu.css({"visibility" : "hidden"});
	}
}

var addCount = 1;

function addTreeNode() {
	hideRMenu();
	var createFile = false;
	var createFolder = false;
	if (confirm("Create a txt file?")==true){
		createFile = true;
	}
	else if (confirm("Create a folder?")==true){
		createFolder = true;
	}
	else{return;}
	
	var newNode = { name:"Add" + (addCount++), isFile:false};
	
	if (zTree.getSelectedNodes()[0]) {
		zTree.addNodes(zTree.getSelectedNodes()[0], newNode);
	} else {
		zTree.addNodes(null, newNode);
	}
		
	createFolderOrFile(createFile, createFolder, zTree.getNodeByParam("name", newNode.name, null));
}

function createFolderOrFile(createFile, createFolder, nodes) {
	cArray = [];
	traversalPN(nodes);
	if(createFile){
		zTree.updateNode(nodes.isFile=true);
		TestCaseSelectorRPC.newFileORDir(tcDirPath.substring(0,tcDirPath.lastIndexOf("\\")) + "\\" + cArray.reverse().join("\\") + ".txt", 
		true, function(t) {
		var res = t.responseObject();
		if(res == null){loadTreeFromPath(tcDirPath, writeFilePath, configPYFile, jenkinsRoot);}
		});
	}
	else if(createFolder){
		zTree.updateNode(nodes.isFile=false);
		TestCaseSelectorRPC.newFileORDir(tcDirPath.substring(0,tcDirPath.lastIndexOf("\\")) + "\\" + cArray.reverse().join("\\"),
		false, function(t) {
		var res = t.responseObject();
		if(res == null){loadTreeFromPath(tcDirPath, writeFilePath, configPYFile, jenkinsRoot);}
		});
	}
}

function removeTreeNode() {
	hideRMenu();
	if (confirm("Delete selected node?")==false){return;}
	cArray = [];
	var nodes = zTree.getSelectedNodes();
	traversalPN(nodes[0]);
	if (nodes && nodes.length>0) {
		if (nodes[0].children && nodes[0].children.length > 0) {
			var msg = "To delete a node is the parent node, if you remove the child together with the deleted nodes together.\n\nPlease Confirm!";
			if (confirm(msg)==true){
				zTree.removeNode(nodes[0]);
			}
		} else {
			zTree.removeNode(nodes[0]);
		}
	}
	if(nodes[0].isFile){
		TestCaseSelectorRPC.deleteFolderFile(tcDirPath.substring(0,tcDirPath.lastIndexOf("\\")) + "\\" + cArray.reverse().join("\\") + ".txt", 
		function(t) {
		var res = t.responseObject();
		if(res){loadTreeFromPath(tcDirPath, writeFilePath, configPYFile, jenkinsRoot);}
		});
	}
	else{
		TestCaseSelectorRPC.deleteFolderFile(tcDirPath.substring(0,tcDirPath.lastIndexOf("\\")) + "\\" + cArray.reverse().join("\\"),
		function(t) {
		var res = t.responseObject();
		if(res){loadTreeFromPath(tcDirPath, writeFilePath, configPYFile, jenkinsRoot);}
		});
	}
}

function editTreeNode() {
	hideRMenu();
	nodes = zTree.getSelectedNodes();
	zTree.editName(nodes[0]);
}

function zTreeBeforeRename(treeId, treeNode, newName, isCancel) {
	cArray = [];
	traversalPN(treeNode);
	var filepath = tcDirPath.substring(0,tcDirPath.lastIndexOf("\\")) + "\\" + cArray.reverse().join("\\") + ".txt";
	var folderpath = tcDirPath.substring(0,tcDirPath.lastIndexOf("\\")) + "\\" + cArray.join("\\");
	var newfilepath = filepath.substring(0,filepath.lastIndexOf("\\")) + "\\" + newName + ".txt";
	var newfolderpath = folderpath.substring(0,folderpath.lastIndexOf("\\")) + "\\" + newName;

	if(treeNode.isFile){
		TestCaseSelectorRPC.renameFolderFile(filepath, newfilepath, function(t) {
		var res = t.responseObject();
		if(res == null){loadTreeFromPath(tcDirPath, writeFilePath, configPYFile, jenkinsRoot);}
		});
	}
	else{
		TestCaseSelectorRPC.renameFolderFile(folderpath, newfolderpath, function(t) {
		var res = t.responseObject();
		if(res == null){loadTreeFromPath(tcDirPath, writeFilePath, configPYFile, jenkinsRoot);}
		});	
	}
}

function zheZhaoDIV(flag){
	if(flag){
		jQuery("#zhezhao").show();
		jQuery("#loading").show();
	}
	else{
		jQuery("#zhezhao").hide();
		jQuery("#loading").hide();
	}
}