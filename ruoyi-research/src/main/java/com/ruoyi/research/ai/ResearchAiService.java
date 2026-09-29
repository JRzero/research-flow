package com.ruoyi.research.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

@Service
public class ResearchAiService {
    @Value("${research.deepseek.endpoint:https://api.deepseek.com}")
    private String endpoint;
    @Value("${research.deepseek.api-key:}")
    private String apiKey;
    @Value("${research.deepseek.model:deepseek-chat}")
    private String model;

    public Map<String, Object> generateProposal(String description) {
        if (StringUtils.isEmpty(description)) {
            throw new ServiceException("请先输入项目描述");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        if (StringUtils.isEmpty(apiKey)) {
            result.put("enabled", false);
            result.put("content", "AI 服务尚未配置。请设置 DEEPSEEK_API_KEY 后使用申报辅助；核心项目管理功能不依赖 AI。\n\n当前描述：" + description);
            return result;
        }

        String systemPrompt = "你是科研项目申报助手。根据用户的原始想法，整理为简洁、克制、可验证的科研项目申报草稿。"
                + "输出严格使用以下四个小标题：项目简介、研究目标、研究内容、预期成果。不要虚构具体数据或已有成果。";
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.3,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", description)));
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = RestClient.create(endpoint)
                    .post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            String content = extractContent(response);
            result.put("enabled", true);
            result.put("content", content);
            return result;
        } catch (Exception e) {
            throw new ServiceException("AI 服务暂时不可用，请稍后重试");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null) return "";
        Object choicesValue = response.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) return "";
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) return "";
        Object messageValue = firstMap.get("message");
        if (!(messageValue instanceof Map<?, ?> messageMap)) return "";
        Object content = messageMap.get("content");
        return content == null ? "" : String.valueOf(content);
    }
}
