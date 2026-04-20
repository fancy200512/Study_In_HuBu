package com.cy.constant;

/**
 * AI prompt constants.
 */
public final class AISystemPromptConstant {

    public static final String DefaultPrompt =
            "请根据我提供的食物图片，完成以下任务：\n" +
                    "\n" +
                    "1. 识别图片中所有食物（可能有多种）\n" +
                    "2. 对每种食物进行合理的重量估算（如果无法确定，请基于常见份量估算）\n" +
                    "3. 计算整张图片中所有食物的总营养数据，包括：\n" +
                    "   - 总热量（kcal）\n" +
                    "   - 蛋白质（g）\n" +
                    "   - 脂肪（g）\n" +
                    "   - 碳水化合物（g）\n" +
                    "\n" +
                    "4. 如果当前估算仍存在明显不确定性，请找出最值得继续补充的 1~3 个食物，每个食物只选择一个最关键的补充类型：\n" +
                    "   - WEIGHT（重量）\n" +
                    "   - COUNT（数量）\n" +
                    "   - COOKING_METHOD（烹饪方式）\n" +
                    "   - NONE（不需要补充）\n" +
                    "\n" +
                    "5. 对每个需要补充的食物，给出 2~4 个可以直接点击选择的简短选项，最后一个选项尽量给“其他/不确定”\n" +
                    "\n" +
                    "要求：\n" +
                    "- 所有数据为“整张图片的总和”（不要分食物输出）\n" +
                    "- 即使信息不完整，也要给出合理估算\n" +
                    "- followUpItems 最多返回 3 项；如果不需要补充，followUpItems 返回 []\n" +
                    "- foodName 必须是图片中真实存在的食物名称，简短明确\n" +
                    "- options 必须是用户可以直接点击的短文案，不要写成长句\n" +
                    "- 一个食物只能选择一种 infoType，不要同时追问重量和数量\n" +
                    "- 如果 followUpItems 不为空，needMoreInfo 返回 followUpItems 第一项的 infoType；否则返回 NONE\n" +
                    "- 营养数值请尽量给出更贴近估算场景的结果：总热量、蛋白质、脂肪、碳水优先保留 1 位小数\n" +
                    "- 除非你非常确定是整数，否则不要把所有数值都估成整十、整五或过于工整的数字\n" +
                    "- 不要解释过程\n" +
                    "- 不要输出多余文字\n" +
                    "\n" +
                    "输出必须是严格JSON格式:\n" +
                    "{\n" +
                    "  \"totalCalories\": number,\n" +
                    "  \"protein\": number,\n" +
                    "  \"fat\": number,\n" +
                    "  \"carbs\": number,\n" +
                    "  \"needMoreInfo\": \"WEIGHT|COUNT|COOKING_METHOD|NONE\",\n" +
                    "  \"followUpItems\": [\n" +
                    "    {\n" +
                    "      \"foodName\": \"string\",\n" +
                    "      \"infoType\": \"WEIGHT|COUNT|COOKING_METHOD\",\n" +
                    "      \"options\": [\"string\", \"string\", \"string\"]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";


    public static final String RefinePrompt =
            "请根据我提供的食物图片，完成以下任务：\n" +
                    "\n" +
                    "1. 识别图片中所有食物（可能有多种）\n" +
                    "2. 对每种食物进行合理的重量估算（如果无法确定，请基于常见份量估算）\n" +
                    "3. 计算整张图片中所有食物的总营养数据，包括：\n" +
                    "   - 总热量（kcal）\n" +
                    "   - 蛋白质（g）\n" +
                    "   - 脂肪（g）\n" +
                    "   - 碳水化合物（g）\n" +
                    "\n" +
                    "4. 如果当前估算仍存在明显不确定性，请找出最值得继续补充的 1~3 个食物，每个食物只选择一个最关键的补充类型：\n" +
                    "   - WEIGHT（重量）\n" +
                    "   - COUNT（数量）\n" +
                    "   - COOKING_METHOD（烹饪方式）\n" +
                    "   - NONE（不需要补充）\n" +
                    "\n" +
                    "5. 对每个需要补充的食物，给出 2~4 个可以直接点击选择的简短选项，最后一个选项尽量给“其他/不确定”\n" +
                    "\n" +
                    "要求：\n" +
                    "- 所有数据为“整张图片的总和”（不要分食物输出）\n" +
                    "- 即使信息不完整，也要给出合理估算\n" +
                    "- followUpItems 最多返回 3 项；如果不需要补充，followUpItems 返回 []\n" +
                    "- foodName 必须是图片中真实存在的食物名称，简短明确\n" +
                    "- options 必须是用户可以直接点击的短文案，不要写成长句\n" +
                    "- 一个食物只能选择一种 infoType，不要同时追问重量和数量\n" +
                    "- 如果 followUpItems 不为空，needMoreInfo 返回 followUpItems 第一项的 infoType；否则返回 NONE\n" +
                    "- 营养数值请尽量给出更贴近估算场景的结果：总热量、蛋白质、脂肪、碳水优先保留 1 位小数\n" +
                    "- 除非你非常确定是整数，否则不要把所有数值都估成整十、整五或过于工整的数字\n" +
                    "- 不要解释过程\n" +
                    "- 不要输出多余文字\n" +
                    "\n" +
                    "输出必须是严格JSON格式:\n" +
                    "{\n" +
                    "  \"totalCalories\": number,\n" +
                    "  \"protein\": number,\n" +
                    "  \"fat\": number,\n" +
                    "  \"carbs\": number,\n" +
                    "  \"needMoreInfo\": \"WEIGHT|COUNT|COOKING_METHOD|NONE\",\n" +
                    "  \"followUpItems\": [\n" +
                    "    {\n" +
                    "      \"foodName\": \"string\",\n" +
                    "      \"infoType\": \"WEIGHT|COUNT|COOKING_METHOD\",\n" +
                    "      \"options\": [\"string\", \"string\", \"string\"]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n" +
                    "\n" +
                    "补充信息如下:";
}
