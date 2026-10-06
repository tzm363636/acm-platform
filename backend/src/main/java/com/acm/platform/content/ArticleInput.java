package com.acm.platform.content;
import com.acm.platform.auth.AuthService;
import java.util.*;
import java.util.regex.Pattern;

/** Plain text sections; never raw HTML. Code strings remain unmodified. */
public record ArticleInput(String title,String summary,Long categoryId,List<Long> tagIds,List<Section> sections,Integer revision,String draftKey) {
 public ArticleInput(String title,String summary,Long categoryId,List<Long> tags,List<Section> sections,Integer revision){this(title,summary,categoryId,tags,sections,revision,null);}
 public record Section(String heading,Integer level,List<String> paragraphs,List<String> bullets,String code,String codeLanguage){
  public Section(String heading,Integer level,List<String> paragraphs,List<String> bullets,String code){this(heading,level,paragraphs,bullets,code,null);}
 }
 private static final Pattern HTML=Pattern.compile("<\\s*/?\\s*[a-zA-Z][^>]*>|javascript\\s*:|on[a-z]+\\s*=",Pattern.CASE_INSENSITIVE);
 static String plain(String value,int min,int max,String field){String v=AuthService.text(value,min,max,field);if(HTML.matcher(v).find())throw AuthService.bad(field+"请使用纯文本，HTML 请放入代码块。");return v;}
 public void validate(){
  plain(title,1,255,"标题");plain(summary,1,2000,"摘要");if(categoryId==null||categoryId<1)throw AuthService.bad("请选择分类。");
  if(draftKey!=null&&!draftKey.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))throw AuthService.bad("草稿创建标识无效。");
  if(tagIds==null||tagIds.size()>8||new HashSet<>(tagIds).size()!=tagIds.size()||tagIds.stream().anyMatch(x->x==null||x<1))throw AuthService.bad("标签最多 8 个，且不能重复。");
  if(sections==null||sections.isEmpty()||sections.size()>40)throw AuthService.bad("正文需要 1–40 个章节。");
  int length=0;
  for(var s:sections){if(s==null)throw AuthService.bad("章节不能为空。");plain(s.heading(),1,160,"章节标题");if(s.level()==null||!Set.of(2,3,4).contains(s.level()))throw AuthService.bad("标题层级须为 H2/H3/H4。");
   if(s.codeLanguage()!=null&&!s.codeLanguage().matches("[a-zA-Z0-9_+-]{1,32}"))throw AuthService.bad("代码语言标识无效。");
   if(s.paragraphs()==null||s.paragraphs().size()>100||s.bullets()!=null&&s.bullets().size()>100)throw AuthService.bad("正文段落或列表过多。");
   for(String p:s.paragraphs()){length+=plain(p,1,12000,"段落").length();}
   if(s.bullets()!=null)for(String p:s.bullets())length+=plain(p,1,2000,"列表项").length();
   if(s.code()!=null){if(s.code().length()>100000||s.code().indexOf('\0')>=0)throw AuthService.bad("代码块过长或包含无效字符。");length+=s.code().length();}
  }
  if(length==0||length>500000)throw AuthService.bad("正文不能为空，总长度不超过 500000 字符。");
 }
}
