// Even a warning example, is an example. :-X

package jenkins.plugins.robottc;

import jenkins.plugins.robottc.GetJsonFromDir;
import hudson.Extension;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.model.Hudson;
import hudson.model.StringParameterValue;

import sp.sd.fileoperations.dsl.FileOperationsJobDslContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

public class TestCaseSelector extends SimpleParameterDefinition {
	private final String uuid = UUID.randomUUID().toString();

	private final String robottcPath;
	private final String argfilePath;
	private final String conffilePath;

	@DataBoundConstructor
	public TestCaseSelector(String name, String robottcPath, String argfilePath, String conffilePath) {

		super(name);
		this.robottcPath = robottcPath;
		this.argfilePath = argfilePath;
		this.conffilePath = conffilePath;
	}

	public String getUuid() {
		return uuid;
	}

	public String getrobottcPath() {
		return robottcPath;
	}

	public String getargfilePath() {
		return argfilePath;
	}

	public String getconffilePath() {
		return conffilePath;
	}

	// Overridden for better type safety.
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) Hudson.getInstance().getDescriptor(getClass());
	}

	public static class RWFileResponse {
		public RWFileResponse(boolean success, String errorMsg, String content) {
			super();
			this.success = success;
			this.errorMsg = errorMsg;
			this.content = content;
		}

		public boolean success;
		public String errorMsg;
		public String content;
	}

	@Extension
	// This indicates to Jenkins that this is an implementation of an extension
	// point.
	public static final class DescriptorImpl extends ParameterDescriptor {
		// This human readable name is used in the configuration screen.
		public String getDisplayName() {
			return "Choosing RF Tests";
		}

		@JavaScriptMethod
		public static RWFileResponse loadRobotTC(String filePath, String argfile) {
			if (filePath == null || filePath.isEmpty()) {
				return new RWFileResponse(false, "Invalid directory name", null);
			}

			File file = new File(filePath);
			File afile = new File(argfile);

			if (!file.exists()) {
				return new RWFileResponse(
						false,
						"Directory doesn't exist OR Jenkins doesn't have permission for this directory",
						null);
			}

			if (!file.isDirectory()) {
				return new RWFileResponse(false, "Not a directory", null);
			}

			try {
				return new RWFileResponse(true, null,
						GetJsonFromDir.robottc2json(file, afile));
			} catch (IOException e) {
				return new RWFileResponse(false, "Could not access directory: "
						+ e.getMessage(), null);
			}
		}

		@JavaScriptMethod
		public static RWFileResponse writeTCConfigFile(String filePath,
				String content) {
			// System.out.println(Hudson.getInstance().getItem(name));
			if (filePath == null || filePath.isEmpty()) {
				return new RWFileResponse(false, "Invalid file name", null);
			}
			String newContent = content.replaceAll(",\\n--test\\n",
					"\n--test\n");
			;
			if (content.equalsIgnoreCase("\n--test\n")) {
				newContent = null;
			}
			newContent = "-C\noff\n-W\n133" + newContent;
			File file = new File(filePath);
			//FileWriter fw = null;
			BufferedWriter writer = null;

			try {
				//fw = new FileWriter(file);
				//writer = new BufferedWriter(fw);
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
				writer.write(newContent);
				writer.flush();
				return new RWFileResponse(true, null, content);
			} catch (IOException e) {
				return new RWFileResponse(false, e.getMessage(), null);
			} finally {
				try {
					writer.close();
					//fw.close();
				} catch (IOException e) {
					return new RWFileResponse(false, e.getMessage(), null);
				}
			}
		}

		@JavaScriptMethod
		public static RWFileResponse writeFile(String fileName, String content) {
			if (fileName == null || fileName.isEmpty()) {
				return new RWFileResponse(false, "Invalid file name", null);
			}
			File file = new File(fileName);
			//FileWriter fw = null;
			BufferedWriter writer = null;
			try {
				//fw = new FileWriter(file);
				//writer = new BufferedWriter(fw);
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
				writer.write(content);
				writer.flush();
				return new RWFileResponse(true, null, content);
			} catch (IOException e) {
				return new RWFileResponse(false, e.getMessage(), null);
			} finally {
				try {
					writer.close();
					//fw.close();
				} catch (IOException e) {
					return new RWFileResponse(false, e.getMessage(), null);
				}
			}
		}
	}

	@Override
	public ParameterValue createValue(String value) {
		return new StringParameterValue(getName(), value);
	}

	@Override
	public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
		// return createValue(jo.getString("selectedTests"));
		String strpath = jo.getString("selectedTests");
		return createValue(strpath.replaceAll("\\\\\\\\", "\\\\"));
	}

	@JavaScriptMethod
	public RWFileResponse loadRobotTC(String filePath, String argfile) {
		return DescriptorImpl.loadRobotTC(filePath, argfile);
	}

	@JavaScriptMethod
	public RWFileResponse writeTCConfigFile(String filePath, String ctext) {
		return DescriptorImpl.writeTCConfigFile(filePath, ctext);
	}

	@JavaScriptMethod
	public RWFileResponse writeFile(String fileName, String ctext) {
		return DescriptorImpl.writeFile(fileName, ctext);
	}

	@JavaScriptMethod
	public boolean copyFile(boolean flag) {
		String tmpStr = "";
		File oldfile = null;
		File newfile = null;
		if (flag) {
			tmpStr = getargfilePath();
			oldfile = new File(tmpStr);
			newfile = new File(oldfile.getParent() + "\\daily"
					+ oldfile.getName());
		} else {
			tmpStr = getargfilePath();
			newfile = new File(tmpStr);
			oldfile = new File(newfile.getParent() + "\\daily"
					+ newfile.getName());
		}
		FileInputStream fi = null;
		FileOutputStream fo = null;
		FileChannel in = null;
		FileChannel out = null;
		if (oldfile.exists()) {
			try {
				fi = new FileInputStream(oldfile);
				fo = new FileOutputStream(newfile);
				in = fi.getChannel();
				out = fo.getChannel();
				in.transferTo(0, in.size(), out);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					fi.close();
					in.close();
					fo.close();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}

	@JavaScriptMethod
	public String readFile(String fileName) {
		//String encoding = "ISO-8859-1";
		String encoding = "UTF-8";
		File file = new File(fileName);
		Long filelength = file.length();
		byte[] filecontent = new byte[filelength.intValue()];
		try {
			FileInputStream in = new FileInputStream(file);
			in.read(filecontent);
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			return new String(filecontent, encoding);
		} catch (UnsupportedEncodingException e) {
			System.err.println("The OS does not support " + encoding);
			e.printStackTrace();
			return null;
		}
	}

	@JavaScriptMethod
	public String newFileORDir(String filePath, boolean flag) {
		File file = new File(filePath);
		if (flag) {
			if (!file.exists()) {
				try {
					//file.createNewFile();
					FileOperationsJobDslContext fojdc = new FileOperationsJobDslContext();
					fojdc.fileCreateOperation(filePath);
					return null;
				} catch (IOException e) {
					return e.toString();
				}
			}
		} else {
			if (!file.exists() && !file.isDirectory()) {
				//file.mkdir();
				FileOperationsJobDslContext fojdc = new FileOperationsJobDslContext();
				fojdc.folderCreateOperation(filePath);
				return null;
			} else {
				return "Directory is exist!";
			}
		}
		return null;
	}

	@JavaScriptMethod
	public boolean deleteFolderFile(String sPath) {
		boolean flag = false;
		File file = new File(sPath);
		if (!file.exists()) {
			return flag;
		} else {
			if (file.isFile()) {
				return deleteFile(sPath);
			} else {
				return deleteDirectory(sPath);
			}
		}
	}

	public boolean deleteFile(String sPath) {
		boolean flag = false;
		File file = new File(sPath);
		if (file.isFile() && file.exists()) {
			file.delete();
			flag = true;
		}
		return flag;
	}

	public boolean deleteDirectory(String sPath) {
		if (!sPath.endsWith(File.separator)) {
			sPath = sPath + File.separator;
		}
		File dirFile = new File(sPath);
		if (!dirFile.exists() || !dirFile.isDirectory()) {
			return false;
		}
		boolean flag = true;
		File[] files = dirFile.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				flag = deleteFile(files[i].getAbsolutePath());
				if (!flag)
					break;
			} else {
				flag = deleteDirectory(files[i].getAbsolutePath());
				if (!flag)
					break;
			}
		}
		if (!flag)
			return false;
		if (dirFile.delete()) {
			return true;
		} else {
			return false;
		}
	}

	@JavaScriptMethod
	public String renameFolderFile(String oldPath, String newPath) {
		File file = new File(oldPath);
		if (file.exists()) {
			file.renameTo(new File(newPath));
			return null;
		}
		return "File is not exist!";
	}
}
