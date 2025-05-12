package com.example.fetcherdemo.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 抓取请求DTO
 * 
 * @author example
 * @date 2023-06-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FetchRequest {
    
    /**
     * 要抓取的URL
     */
    @NotBlank(message = "URL不能为空")
    private String url;
    
    /**
     * 是否提取正文内容
     */
    private boolean extractContent;
    
    /**
     * 是否保留HTML标签
     */
    private boolean keepHtml;
    
    /**
     * 提取规则，CSS选择器或XPath
     */
    private String extractRule;
    
    /**
     * 提取规则类型：CSS或XPATH
     */
    private String ruleType;
    
    /**
     * HTTP请求头
     */
    private Map<String, String> headers;
    
    /**
     * 请求超时时间（毫秒）
     */
    private Integer timeout;
    
    /**
     * 规则类型枚举
     */
    public enum RuleType {
        /**
         * CSS选择器
         */
        CSS,
        
        /**
         * XPath表达式
         */
        XPATH
    }
} 