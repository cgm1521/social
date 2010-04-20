package org.exoplatform.social.core.activitystream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.social.core.activitystream.model.Activity;

public class OSHtmlSanitizerProcessor extends BaseActivityProcessorPlugin {

 
  /**
   * html tags allowed in title and body by OpenSocial spec
   */
  public static final String[] OS_ALLOWED_TAGS = {"b", "i", "a", "span"};
  
  private String [] allowedTags = OS_ALLOWED_TAGS;
  
  public OSHtmlSanitizerProcessor(InitParams params) {
    super(params);
    allowedTags = getAllowedTags(params, allowedTags) ;
  }
  
  @SuppressWarnings("unchecked")
  private String[] getAllowedTags(InitParams initParams, String[] defaultValue) {
    String [] result = defaultValue;
    try {
      ValuesParam params = initParams.getValuesParam("allowedTags");
       ArrayList<String> values = params.getValues();
       result =  values.toArray(new String [values.size()]);
    }
    catch (Exception e) {
      return defaultValue;
    }
    return result;
  }

  public int getPriority() {
    return Integer.MIN_VALUE;
  }

  public void processActivity(Activity activity) {
    activity.setTitle(escapeHtml(activity.getTitle()));
    activity.setBody(escapeHtml(activity.getBody()));
  }
  
  
  String escapeHtml(String content) {
    if (content == null) {
      return null;
    }

    String result = content;

    String startTag = "<([^<]+)>|<([^<\\s]+)[\\s]+[^<]+>";
    String endTag = "</([^<]+)>";
    Pattern pattern = Pattern.compile(startTag + "|"  + endTag);
    Matcher matcher = pattern.matcher(content);

    // Replace all occurrences of pattern in input
    StringBuffer buf = new StringBuffer();
    boolean found = false;
    while ((found = matcher.find())) {
      // Get the match result
      String replaceStr = matcher.group();

      // strip < and >
      String tag;
      if (replaceStr.startsWith("</")) {
        tag = replaceStr.trim().substring(2, replaceStr.length() -1);
        if (!Arrays.asList(allowedTags).contains(tag)) {
          replaceStr = "&lt;/" + tag + "&gt;";
        }
      } else {
        if (replaceStr.matches("<([^<\\s]+)[\\s]+[^<]+>")) {
          String start = replaceStr.split("\\s")[0];
          tag = start.substring(1);
        } else {
          tag = replaceStr.trim().substring(1, replaceStr.length() -1);
        }
        
        
        
        
        if (!Arrays.asList(allowedTags).contains(tag)) {
          replaceStr = "&lt;" + replaceStr.substring(1, replaceStr.length() - 1) + "&gt;";
        }
      }
      
     

      // Insert replacement
      matcher.appendReplacement(buf, replaceStr);
    }
    matcher.appendTail(buf);
    result = buf.toString();
    return result;
  }

  public String[] getAllowedTags() {
    return allowedTags;
  }

  public void setAllowedTags(String[] allowedTags) {
    this.allowedTags = allowedTags;
  }

}