package jenkins.plugins.robottc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public class GetJsonFromDir {
	
	public static class FileTree {

		private Node mRoot = null;

		public class Node {
			String name = "";
			List<Node> children = new ArrayList<Node>();
			boolean isParent = true;
			boolean open = false;
			boolean isCase = false;
			boolean checked = false;
			boolean isFile = false;
			boolean noR = false;
			String icon = "";
			String parent = "";
		}

		private void createTree(File file, List<String> selectNodesList) throws IOException {
			mRoot = new Node();
			mRoot.name = file.getName();
			mRoot.parent = mRoot.name;
			mRoot.open = true;
			
			for(String citem : selectNodesList)
			{
				if(citem.contains(mRoot.parent + "."))
				{
					mRoot.checked = true;
					break;
				}
			}
			/*if (selectNodesList.contains(mRoot.name)){
				mRoot.checked = true;
			}*/
			getChildren(mRoot, file, selectNodesList);

		}

		private void getChildren(Node node, File file, List<String> selectNodesList) throws IOException {
			File[] files = file.listFiles();
			if (files == null || files.length <= 0)
				return;

			for (File ifile : files) {
				Node child = new Node();
				child.name = ifile.getName();
				child.parent = node.parent + "." + child.name;
				node.children.add(child);
				for(String citem : selectNodesList)
				{
					if(citem.contains(child.parent + "."))
					{
						child.checked = true;
						break;
					}
				}				

				if (ifile.isFile() && ifile.getName().endsWith("txt")) {
					child.name = ifile.getName().substring(0, ifile.getName().lastIndexOf("."));
					child.parent = node.parent + "." + child.name;
					for(String citem : selectNodesList)
					{
						if(citem.contains(child.parent + "."))
						{
							child.checked = true;
							break;
						}
					}
					boolean ret = handleCase(child, ifile, selectNodesList);
					if (!ret) {
						//node.children.remove(child);
						child.isFile = true;
						child.isParent = false;
						child.icon = "rootURL/plugin/robottc/images/file.png";
					}
				} else if (ifile.isHidden() || ifile.isFile()) {
					node.children.remove(child);
				}
				if (ifile.isDirectory()) {
					getChildren(child, ifile, selectNodesList);
				}
			}
		}

		public String treeToJson() {
			if (mRoot == null)
				return "";
			Gson gson = new Gson();
			return gson.toJson(mRoot);
		}

		private boolean handleCase(Node node, File file, List<String> selectNodesList) throws IOException {
			List<String> caseList = getCasesFromFile(file);
			if (caseList.size() > 0) {
				for (String testCase : caseList) {
					Node cNode = new Node();
					cNode.name = testCase;
					cNode.parent = node.parent + "." + cNode.name;
					for(String citem : selectNodesList)
					{
						if(citem.contains(cNode.parent))
						{
							cNode.checked = true;
							break;
						}
					}
					cNode.isParent = false;
					cNode.isCase = true;
					cNode.noR = true;
					cNode.icon = "rootURL/plugin/robottc/images/robot.png";
					node.children.add(cNode);
					node.icon = "rootURL/plugin/robottc/images/file.png";
					node.isFile = true;
				}
				return true;
			} else {
				return false;
			}
		}
	}

	public static List<String> getCasesFromFile(File file) throws IOException {
		boolean inflag = false;
		ArrayList<String> cList = new ArrayList<String>();
		BufferedReader reader = null;
		try {
			//reader = new BufferedReader(new FileReader(file));
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String lineStr = null;
			while ((lineStr = reader.readLine()) != null) {
				if (lineStr.length() < 1) continue;
				if (lineStr.matches("\\*+\\s*Test\\s*Cases\\s*\\*+") || inflag) {
					inflag = true;
					if (!lineStr.startsWith(" ") && !lineStr.startsWith("*")) {
						String c = lineStr;
						cList.add(c);
					}
					else if(lineStr.matches("\\*+\\s*Keywords\\s*\\*+")){
						inflag = false;
						break;
					}
				}
			}
			reader.close();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		return cList;
	}
	
	public static List<String> getSelectedCasesFromFile(File file) throws IOException {
		ArrayList<String> scList = new ArrayList<String>();
		BufferedReader reader = null;
		if (!file.exists()) {
			return scList;
		}
		try {
			//reader = new BufferedReader(new FileReader(file));
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String lineStr = null;
			while ((lineStr = reader.readLine()) != null) {
				if (lineStr.length() < 1) continue;
				if (lineStr.matches("(.+\\.)+.+")) {
					/*String tmp[] = lineStr.split("\\.");
					for(String item : tmp){
						if (!scList.contains(item)){
							scList.add(item);
						}
					}*/
					scList.add(lineStr);
				}
			}
			reader.close();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		return scList;
	}

	public static String robottc2json(File file, File argfile) throws IOException {
		FileTree tree = new FileTree();
		tree.createTree(file, getSelectedCasesFromFile(argfile));
		return tree.treeToJson();
	}
}