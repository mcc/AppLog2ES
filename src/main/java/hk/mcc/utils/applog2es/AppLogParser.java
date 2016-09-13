package hk.mcc.utils.applog2es;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author cc
 */
public class AppLogParser implements Closeable {
    private static final Logger LOGGER = Logger.getLogger(AppLogParser.class.getName());
    final LineNumberReader reader;
    String lastLine = null;
    long lineNumber = 0;
    long logLineNumber = 0;
    private static final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    public AppLogParser(InputStream inputStream) throws UnsupportedEncodingException, IOException {
        reader = new LineNumberReader(new InputStreamReader(inputStream, "UTF-8"));
        lastLine = reader.readLine();
        lineNumber++;
        LOGGER.info("Finish Init" + new String(lastLine));
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private String nextLogString() throws IOException {
        if (lastLine != null) {
            String currentLog = lastLine;
            logLineNumber = lineNumber;
            String mutilLine = moveToNextLog();
            if (mutilLine != null) {
                currentLog = currentLog + "\n" + mutilLine;
            }
            return currentLog;
        } else {
            return null;
        }
    }

    public AppLog nextLog() {
        try {
            AppLog logData = null;
            String nextLogString = nextLogString();
            LOGGER.info("nextLogString = " + nextLogString);
            if (nextLogString != null) {
                logData = smartParseApplicationLog(nextLogString);
            }
            return logData;
        } catch (Exception ex) {
            //error torrent
            LOGGER.throwing("error parsing log, break at line = " + lineNumber, "nextLog",ex);
            AppLog appLog = new AppLog();
            appLog.setMessage("LOG Parsing ERROR");
            return appLog;
        }
    }

    private AppLog smartParseApplicationLog(String nextLogString) {
        String[] tempArrs = StringUtils.splitByWholeSeparatorPreserveAllTokens(nextLogString, "] [");
        int indexOf = StringUtils.indexOf(tempArrs[tempArrs.length - 1], ']');
        String[] arrs = null;
        if (indexOf > -1) {
            String before = StringUtils.substringBefore(tempArrs[tempArrs.length - 1], "]");
            String after = StringUtils.substringAfter(tempArrs[tempArrs.length - 1], "]");
            tempArrs[tempArrs.length - 1] = before;
            arrs = (String[]) ArrayUtils.add(tempArrs, after);
        } else {
            arrs = (String[]) ArrayUtils.clone(tempArrs);
        }

        AppLog logData = new AppLog();
        //logData.setLine(this.logLineNumber + "");
        if (arrs[0].length() > 0) {
            arrs[0] = arrs[0].substring(1);
        }
        int maxParsedIndex = -1;
        boolean logDateParsed = false;
        boolean logLevelParsed = false;
        boolean logMessageIdParsed = false;
        boolean logThreadParsed = false;
        boolean logSessionIdParsed = false;
        boolean logUserIdParsed = false;
        boolean logApplicationParsed = false;
        Map<String, String> properties = new HashMap<>();
        for (int i = 0; i < arrs.length; i++) {
            boolean parsed = false;
            if (!parsed && !logDateParsed) {
                try {
                    SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat(dateFormat, Locale.US);
                    logData.setLogTime(ISO8601DATEFORMAT.parse(arrs[i]));
                    parsed = true;
                } catch (ParseException e) {
                    // DO nothing
                }
            }
            if (!parsed && !logLevelParsed) {
                Level level = convertLogLevel(arrs[i]);
                if (level != null) {
                    logData.setLevel(arrs[i]);
                    parsed = true;
                }
            }
            if (!parsed && !logMessageIdParsed) {
                if (isMessageId(arrs[i])) {
                    logData.setCode(arrs[i]);
                    parsed = true;
                }
            }
            if (!parsed && !logThreadParsed) {
                String threadStr = convertSimpleThreadStr(arrs[i]);
                if (threadStr != null) {
                    logData.setTid(threadStr);
                    parsed = true;
                }
            }
            if (!parsed && !logUserIdParsed) {
                String userIdStr = convertSimpleUserIdStr(arrs[i]);
                if (userIdStr != null) {
                    logData.setUserId(userIdStr);
                    parsed = true;
                }
            }
            if (!parsed && !logSessionIdParsed) {
                String sessionIdStr = convertSimpleSessionId(arrs[i]);
                if (sessionIdStr != null) {
                    logData.setEcid(sessionIdStr);
                    parsed = true;
                }
            }
            if (!parsed && !logApplicationParsed) {
                String applicationStr = convertSimpleApplication(arrs[i]);
                if (applicationStr != null) {
                    properties.put("application", applicationStr);
                    parsed = true;
                }
            }
            if (!parsed && i == 2) {
                //assume is Server
                properties.put("application", arrs[i]);
                parsed = true;
            }
            if (!parsed && i == 3) {
                //assume is Message Code
                logData.setCode(arrs[i]);
                parsed = true;
            }
            if (!parsed && i == 4) {
                //assume is Class
                logData.setClassName(arrs[i]);
                parsed = true;
            }
            if (parsed) {
                maxParsedIndex = i;
            }
        }
        if (maxParsedIndex + 1 < arrs.length) {
            String message = arrs[maxParsedIndex + 1];
            for (int i = maxParsedIndex + 2; i < arrs.length; i++) {
                message += ("] [" + arrs[i]); //concat remaining arrays
            }
            //message = message + "\n";
            logData.setMessage(message);
        }

        return logData;
    }

    private String convertSimpleApplication(String arr) {
        if (StringUtils.startsWithIgnoreCase(arr, "APP:")) {
            return StringUtils.trim(StringUtils.substringAfter(arr, ":"));
        }
        return null;
    }

    private String convertSimpleSessionId(String arr) {
        if (StringUtils.startsWithIgnoreCase(arr, "ecid:")) {
            return StringUtils.trim(StringUtils.substringAfter(arr, ":"));
        }
        return null;
    }

    private String convertSimpleUserIdStr(String arr) {
        if (StringUtils.startsWithIgnoreCase(arr, "userId:")) {
            return StringUtils.trim(StringUtils.substringAfter(arr, ":"));
        }
        return null;
    }

    private String convertSimpleThreadStr(String arr) {
        if (StringUtils.startsWithIgnoreCase(arr, "tid:")) {
            String status = StringUtils.substringBetween(arr, "[", "]");
            String[] infos = StringUtils.substringsBetween(arr, "'", "'");
            String output = infos[0] + "," + status;
            if (infos.length > 1) {
                output = output + "," + infos[1];
            }
            return output;
        }
        return null;
    }

    private String moveToNextLog() throws IOException {
        String message = null;
        lastLine = reader.readLine();
        lineNumber++;
        while (lastLine != null && !StringUtils.startsWith(lastLine, "[")) {
            if (message == null) {
                message = lastLine;
            } else {
                message = message + "\n" + lastLine;
            }
            lastLine = reader.readLine();
            lineNumber++;
        }
        return message;
    }

    private Level convertLogLevel(String levelString) {
        try {
            return Level.parse(levelString);
        } catch (Exception ex) {
            LOGGER.fine("Cannot Concert Log Level with java.lang.Level");
        }
        if (StringUtils.startsWithIgnoreCase(levelString, "NOTIF")) {
            return Level.INFO;
        } else if (StringUtils.startsWithIgnoreCase(levelString, "WARN")) {
            return Level.WARNING;
        } else if (StringUtils.startsWithIgnoreCase(levelString, "INFO")) {
            return Level.INFO;
        } else if (StringUtils.startsWithIgnoreCase(levelString, "DEBUG")) {
            return Level.FINE;
        } else if (StringUtils.startsWithIgnoreCase(levelString, "ERROR")) {
            return Level.SEVERE;
        }
        return null;
    }

    private boolean isMessageId(String messageString) {
        if (StringUtils.startsWithIgnoreCase(messageString, "JBO-") ||
                StringUtils.startsWithIgnoreCase(messageString, "ADFC-") ||
                StringUtils.startsWithIgnoreCase(messageString, "J2EE JMX-")) {
            return true;
        }
        return false;
    }


}

