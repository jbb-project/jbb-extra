package org.jbb.utils;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Fix_MeEnforcerRule implements EnforcerRule {
    private boolean skip = false;
    private String filesToScan = "**/*.java,**/*.xml,**/*.properties,**/*.html,**/*.js";
    private boolean checkTestSource = true;
    private String excluded = "**/target/**";
    private String encoding;
    private Log log;

    private List<String> filesToScanList = new ArrayList<String>();

    public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
        log = helper.getLog();

        if(skip){
            return;
        }

        if(!checkTestSource){
            excluded = excluded + ",**/src/test/**";
        }

        try {
            String baseDir = helper.evaluate( "${project.basedir}" ) + File.separator;
            listFilesForFolder(new File(baseDir));
            Iterator<String> fileIterator = filesToScanList.iterator();
            while (fileIterator.hasNext()){
                scanFile(new File(fileIterator.next()));
            }

        } catch (ExpressionEvaluationException e) {
            throw new EnforcerRuleException("Error during evaluation project.basedir property", e);
        }
    }

    private void scanFile(File file) throws EnforcerRuleException {
        log.debug("Scan for fixme in file: " + file.getAbsolutePath());
        if(file.exists()){
            LineNumberReader reader = null;
            try {
                reader = new LineNumberReader( getReader( file ) );
                int counter = 1;
                String currentLine = reader.readLine();
                while ( currentLine != null ) {
                    if(containsIgnoreCase(currentLine, "FIXME")){
                        throw new EnforcerRuleException("FIXME detected in file '"+file.getAbsolutePath()+"' in line "+counter+":\n"+currentLine);
                    }
                    currentLine = reader.readLine();
                    counter++;
                }

            } catch (IOException e) {
                throw new EnforcerRuleException("I/O error during reading file: ", e);
            }
        }
    }

    private Reader getReader(File file ) throws IOException
    {
        InputStream in = new FileInputStream( file );
        return ( encoding == null ) ? new InputStreamReader( in ) : new InputStreamReader( in, encoding );
    }

    public boolean isCacheable() {
        return false;
    }

    public boolean isResultValid(EnforcerRule enforcerRule) {
        return false;
    }

    public String getCacheId() {
        return skip+filesToScan;
    }

    private void listFilesForFolder(final File folder) throws EnforcerRuleException {
        if(folder.exists()){
            try {
                List<String> fileNames = FileUtils.getFileNames(folder, filesToScan, excluded, true);
                filesToScanList.addAll(fileNames);

            } catch (IOException e) {
                throw new EnforcerRuleException("Error while getting file names for folder " + folder.getAbsolutePath(), e);
            }
        }
    }

    public static boolean containsIgnoreCase(final CharSequence str, final CharSequence searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        final int len = searchStr.length();
        final int max = str.length() - len;
        for (int i = 0; i <= max; i++) {
            if (regionMatches(str, true, i, searchStr, 0, len)) {
                return true;
            }
        }
        return false;
    }

    static boolean regionMatches(final CharSequence cs, final boolean ignoreCase, final int thisStart,
                                 final CharSequence substring, final int start, final int length)    {
        if (cs instanceof String && substring instanceof String) {
            return ((String) cs).regionMatches(ignoreCase, thisStart, (String) substring, start, length);
        }
        int index1 = thisStart;
        int index2 = start;
        int tmpLen = length;

        while (tmpLen-- > 0) {
            final char c1 = cs.charAt(index1++);
            final char c2 = substring.charAt(index2++);

            if (c1 == c2) {
                continue;
            }

            if (!ignoreCase) {
                return false;
            }

            // The same check as in String.regionMatches():
            if (Character.toUpperCase(c1) != Character.toUpperCase(c2)
                    && Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
                return false;
            }
        }

        return true;
    }
}
