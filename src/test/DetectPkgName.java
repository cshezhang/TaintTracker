package test;

import org.xmlpull.v1.XmlPullParserException;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DetectPkgName {

    public static void getAllFileName(String path, ArrayList<String> fileNameList) {
        //ArrayList<String> files = new ArrayList<String>();
        File file = new File(path);
        File[] tempList = file.listFiles();

        for (int i = 0; i < tempList.length; i++) {
            if (tempList[i].isFile()) {
//              System.out.println("文     件：" + tempList[i]);
                //fileNameList.add(tempList[i].toString());
                fileNameList.add(path + tempList[i].getName());
            }
            if (tempList[i].isDirectory()) {
//              System.out.println("文件夹：" + tempList[i]);
                getAllFileName(tempList[i].getAbsolutePath(), fileNameList);
            }
        }
    }

    public static void main(String[] args) throws IOException, XmlPullParserException {
        String folder = args[0];
        ArrayList<String> apkList = new ArrayList<>();
        getAllFileName(folder, apkList);
//        for(String apk : apkList) {
//            System.out.println(apk);
//        }
        for(String apk : apkList) {
            ProcessManifest manifest = new ProcessManifest(apk);
            String packageName = manifest.getManifest().getAttribute("package").toString();
            packageName = packageName.substring(9, packageName.length() - 1);
            System.out.println(packageName + ".index");
//            System.out.println("APK Name=" + apk + "  Package Name=" + packageName);
        }
    }
}
