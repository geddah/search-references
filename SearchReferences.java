package com.equifax.search.main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SearchReferences
{
  private static SearchReferences fileSearch = new SearchReferences();
  private String fileNameToSearch;
  private List<String> result = new ArrayList();
  private static ReferencesArrayList<String> references = new ReferencesArrayList();
  private static ArrayList<String> falsePositives = new ArrayList();
  private static List<String> jarFiles = new ArrayList();
  private static String sourceDir;
  private static File logFile;
  private static BufferedWriter bwlog;
  
  public String getFileNameToSearch()
  {
    return this.fileNameToSearch;
  }
  
  public void setFileNameToSearch(String fileNameToSearch)
  {
    this.fileNameToSearch = fileNameToSearch;
  }
  
  public List<String> getResult()
  {
    return this.result;
  }
  
  public static void main(String[] args)
  {
    try
    {
      if (sourceDir == null) {
        sourceDir = "C:\\Workspaces\\Sprint4\\components\\New folder";
      }
      String destDir = "./report";
      File destination = new File(destDir);
      File extractsDestination = new File(sourceDir + "\\extracts\\");
      extractsDestination.mkdirs();
      destination.mkdirs();
      fileSearch.searchDirectory(new File(sourceDir));
      
      int count = fileSearch.getResult().size();
      if (count == 0)
      {
        System.out.println("\nNo result found!");
      }
      else
      {
        for (String jarFile : jarFiles) {
          if (jarFile.endsWith(".jar")) {
            fileSearch.extractJarFile(jarFile, sourceDir + "\\extracts\\");
          } else if (jarFile.endsWith(".zip")) {
            fileSearch.extractZipFile(jarFile, sourceDir + "\\extracts\\");
          }
        }
        fileSearch.searchDirectory(extractsDestination);
        System.out.println("\nFound " + count + " result!\n");
        bwlog.write("\nFound " + count + " result!\n");
        File reportHtml = new File(destination + "/Report.html");
        reportHtml.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(reportHtml.getAbsoluteFile()));
        
        initializeHTML(reportHtml, bw);
        List<String> result = fileSearch.getResult();
        float totalSize = result.size();
        float index = 0.0F;
        float status = 1.0F;
        boolean hasAtleastOneEntry = false;
        for (String matched : result)
        {
          index += 1.0F;
          float percentage = index / totalSize * 100.0F;
          if (percentage < Math.ceil(status))
          {
            status = percentage;
          }
          else
          {
            System.out.println("Percentage complete: " + Math.ceil(status) + "%");
            status = percentage;
          }
          bwlog.write("Percentage complete: " + index / totalSize * 100.0F);
          bwlog.write("\n");
          if ((!matched.endsWith(".jar")) && (!matched.endsWith(".zip")))
          {
            BufferedReader br = new BufferedReader(new FileReader(new File(matched)));
            int i = 1;
            boolean isHit = false;
            
            StringBuffer fileContent = new StringBuffer();
            while (br.ready())
            {
              String line = br.readLine();
              line = line.replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;")
                .replaceAll("'", "&apos;");
              StringBuffer lineBuffer = new StringBuffer(line);
              boolean lineHit = false;
              for (String referenceWord : references) {
                if (lineBuffer.toString().toLowerCase().contains(referenceWord.toLowerCase())) {
                  if (!lineBuffer.toString().toLowerCase().contains("<mark>" + referenceWord.toLowerCase() + "</mark>"))
                  {
                    int referenceWordIndex = -1;
                    do
                    {
                      int nextIndexReference = 0;
                      if (lineHit)
                      {
                        nextIndexReference = referenceWordIndex + 12 + String.valueOf(i).length();
                        referenceWordIndex = lineBuffer.toString().toLowerCase()
                          .indexOf(referenceWord.toLowerCase().trim(), nextIndexReference);
                      }
                      else
                      {
                        nextIndexReference = referenceWordIndex;
                        referenceWordIndex = lineBuffer.toString().toLowerCase()
                          .indexOf(referenceWord.toLowerCase().trim(), nextIndexReference);
                      }
                      boolean isFalsePositive = false;
                      for (String falsePositive : falsePositives)
                      {
                        int falsePositiveIndex = lineBuffer.toString().toLowerCase()
                          .indexOf(falsePositive.toLowerCase().trim(), nextIndexReference);
                        if ((falsePositiveIndex != -1) && (falsePositiveIndex <= referenceWordIndex)) {
                          if (referenceWordIndex <= falsePositiveIndex + falsePositive.length())
                          {
                            isFalsePositive = true;
                            break;
                          }
                        }
                      }
                      if (isFalsePositive)
                      {
                        referenceWordIndex += referenceWord.length();
                        if (referenceWordIndex > lineBuffer.length()) {
                          referenceWordIndex = -1;
                        }
                      }
                      else if (referenceWordIndex != -1)
                      {
                        referenceWord = superReferenceWordInList(referenceWord, line);
                        isHit = true;
                        lineHit = true;
                        lineBuffer.insert(referenceWordIndex, "<mark>");
                        lineBuffer.insert(referenceWordIndex + referenceWord.length() + 6, "</mark>");
                        
                        referenceWordIndex += referenceWord.length();
                        if (referenceWordIndex > lineBuffer.length()) {
                          referenceWordIndex = -1;
                        }
                      }
                    } while (referenceWordIndex != -1);
                    if (lineHit) {
                      fileContent.append("<li>" + i + ": " + lineBuffer + "</li>\n");
                    }
                  }
                }
              }
              i++;
            }
            if (isHit)
            {
              bwlog.write("Found matches in: " + matched + "\n");
              bw.write("<h4>" + matched + "</h4>\n");
              bw.write("<ul>\n");
              bw.write(fileContent.toString());
              bw.write("</ul><br><br>\n\n");
              hasAtleastOneEntry = true;
            }
            br.close();
          }
        }
        if (!hasAtleastOneEntry) {
          emptyReport(reportHtml, bw);
        }
        endHTML(reportHtml, bw);
        bw.flush();
        bw.close();
      }
      System.out.println("Search complete. Please find the report in report/Report.html");
      bwlog.flush();
      bwlog.close();
    }
    catch (IOException localIOException) {}
  }
  
  public void searchDirectory(File directory)
    throws IOException
  {
    System.out.println("Searching in: " + directory);
    bwlog.write("Searching in: " + directory + "\n");
    if (directory.isDirectory()) {
      search(directory);
    } else {
      System.out.println(directory.getAbsoluteFile() + " is not a directory!");
    }
  }
  
  private void search(File file)
  {
    try
    {
      if (file.isDirectory()) {
        if (file.canRead())
        {
          File[] arrayOfFile;
          int j = (arrayOfFile = file.listFiles()).length;
          for (int i = 0; i < j; i++)
          {
            File temp = arrayOfFile[i];
            if (temp.isDirectory()) {
              search(temp);
            } else if ((temp.getAbsolutePath().endsWith(".jar")) || (temp.getAbsolutePath().endsWith(".zip"))) {
              jarFiles.add(temp.getAbsolutePath());
            } else {
              this.result.add(temp.getCanonicalFile().toString());
            }
          }
        }
        else
        {
          System.out.println(file.getAbsoluteFile() + "Permission Denied");
        }
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  private void extractJarFile(String jarFile, String destPath)
  {
    try
    {
      JarFile jar = new JarFile(jarFile);
      
      Enumeration enumEntries = jar.entries();
      
      String jarFileName = jarFile.split("\\\\")[(jarFile.split("\\\\").length - 1)];
      String destDir = destPath + jarFileName.substring(0, jarFileName.length() - 4);
      while (enumEntries.hasMoreElements())
      {
        JarEntry file = (JarEntry)enumEntries.nextElement();
        File f = new File(destDir + File.separator + file.getName());
        if (file.isDirectory())
        {
          f.mkdirs();
        }
        else
        {
          InputStream is = jar.getInputStream(file);
          BufferedInputStream bis = new BufferedInputStream(is);
          if (!f.exists())
          {
            System.out.println(f);
            bwlog.write(f + "\n");
            f.getParentFile().mkdirs();
            f.createNewFile();
          }
          FileOutputStream fos = new FileOutputStream(f);
          BufferedOutputStream bos = new BufferedOutputStream(fos);
          while (bis.available() > 0) {
            bos.write(is.read());
          }
          bos.close();
          bis.close();
        }
      }
    }
    catch (IOException io)
    {
      io.printStackTrace();
    }
  }
  
  private void extractZipFile(String zipFile, String destPath)
  {
    byte[] buffer = new byte['?'];
    String jarFileName = zipFile.split("\\\\")[(zipFile.split("\\\\").length - 1)];
    
    String destDir = destPath + jarFileName.substring(0, jarFileName.length() - 4);
    try
    {
      File folder = new File(destDir);
      if (!folder.exists()) {
        folder.mkdirs();
      }
      ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
      
      ZipEntry ze = zis.getNextEntry();
      while (ze != null)
      {
        String fileName = ze.getName();
        File newFile = new File(destDir + File.separator + fileName);
        
        new File(newFile.getParent()).mkdirs();
        if (!newFile.exists()) {
          newFile.createNewFile();
        }
        if (newFile.isFile())
        {
          FileOutputStream fos = new FileOutputStream(newFile);
          int len;
          while ((len = zis.read(buffer)) > 0)
          {
            int len;
            fos.write(buffer, 0, len);
          }
          fos.close();
        }
        ze = zis.getNextEntry();
      }
      zis.closeEntry();
      zis.close();
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
  }
  
  private static void initializeHTML(File report, BufferedWriter bw)
  {
    try
    {
      bw.write("<html><head><title>report</title></head><body><div style = 'width: 100%; word-wrap: break-word;'><h2 style='color: red'>Please delete the generated 'extracts' folder after use.</h2></br></br>");
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  private static void endHTML(File report, BufferedWriter bw)
  {
    try
    {
      bw.write("</div></body></html>");
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  private static void emptyReport(File report, BufferedWriter bw)
  {
    try
    {
      bw.write("<h2>There are no references in the File System.</h2>");
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  private static String superReferenceWordInList(String referenceWord, String line)
  {
    ReferencesArrayList<String> superReferences = references.superStringsInList(referenceWord);
    boolean isHit = false;
    for (String superReference : superReferences) {
      if (line.contains(superReference))
      {
        isHit = true;
        referenceWord = superReference;
      }
    }
    if (isHit) {
      superReferenceWordInList(referenceWord, line);
    }
    return referenceWord;
  }
}
