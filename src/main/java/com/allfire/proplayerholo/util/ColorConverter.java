package com.allfire.proplayerholo.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Универсальный конвертер цветовых форматов → MiniMessage.
 * Поддерживает форматы из PlaceholderAPI, старых плагинов, CMI, ItemsAdder и т.д.
 */
public final class ColorConverter {

    private ColorConverter() {}

    // ──── Паттерны для всех форматов ────

    // Legacy &: &c, &l, &f, &r и т.д.
    private static final Pattern LEGACY_AMPERSAND =
            Pattern.compile("&([0-9a-fA-Fk-oK-OrR])");

    // Legacy HEX &: &x&F&F&5&7&3&3
    private static final Pattern LEGACY_HEX_AMPERSAND =
            Pattern.compile("&x((?:&[0-9a-fA-F]){6})");

    // Ванильный §: §c, §l, §f, §r
    private static final Pattern VANILLA_SECTION =
            Pattern.compile("§([0-9a-fA-Fk-oK-OrR])");

    // Ванильный HEX §: §x§F§F§5§7§3§3
    private static final Pattern VANILLA_HEX_SECTION =
            Pattern.compile("§x((?:§[0-9a-fA-F]){6})");

    // Простой HEX: &#FF5733 (1.16+ Paper)
    private static final Pattern SIMPLE_HEX =
            Pattern.compile("&#([0-9a-fA-F]{6})");

    // CMI / ItemsAdder HEX: {#FF5733}
    private static final Pattern CMI_HEX =
            Pattern.compile("\\{#([0-9a-fA-F]{6})\\}");

    // CMI градиент: {#FF5733>}текст{#000000<}
    private static final Pattern CMI_GRADIENT_START =
            Pattern.compile("\\{#([0-9a-fA-F]{6})>\\}");

    private static final Pattern CMI_GRADIENT_END =
            Pattern.compile("\\{#([0-9a-fA-F]{6})<\\}");

    // Уже существующие MiniMessage закрывающие теги (для пропуска)
    private static final Pattern MINIMESSAGE_TAG =
            Pattern.compile("</?[a-z_]+>|</?#[0-9a-fA-F]{6}>|</?gradient:[^>]+>|</?rainbow>");

    // ──── Маппинг legacy кодов → MiniMessage ────

    private static String legacyCodeToMiniMessage(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> "";
        };
    }

    // ──── Извлечение HEX из legacy/ванильных паттернов ────

    private static String extractHex(String matched, String delimiter) {
        return matched.replace(delimiter, "").replace("&", "").replace("§", "");
    }

    // ──── Главный метод конвертации ────

    /**
     * Конвертирует строку с ЛЮБЫМ известным форматом цветов в MiniMessage.
     * Поддерживает: &f, §f, &#FF5733, &x&F&F..., §x§F§F..., {#FF5733}, {#F>}text{#0<}
     * Уже существующие MiniMessage теги остаются без изменений.
     */
    public static String toMiniMessage(String input) {
        if (input == null || input.isEmpty()) return input;

        String result = input;

        // === Шаг 1: CMI формат {#FF5733>}открывающий градиент{#000000<}закрывающий ===
        result = convertCmiGradient(result);

        // === Шаг 2: CMI HEX {#FF5733} → <#FF5733> ===
        result = CMI_HEX.matcher(result).replaceAll("<#$1>");

        // === Шаг 3: Простой HEX &#FF5733 → <#FF5733> ===
        result = SIMPLE_HEX.matcher(result).replaceAll("<#$1>");

        // === Шаг 4: Legacy HEX &x&F&F... → <#FF5733> ===
        Matcher legacyHexMatcher = LEGACY_HEX_AMPERSAND.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (legacyHexMatcher.find()) {
            String hex = extractHex(legacyHexMatcher.group(), "&x&");
            legacyHexMatcher.appendReplacement(sb, "<#" + hex + ">");
        }
        legacyHexMatcher.appendTail(sb);
        result = sb.toString();

        // === Шаг 5: Ванильный HEX §x§F§F... → <#FF5733> ===
        Matcher vanillaHexMatcher = VANILLA_HEX_SECTION.matcher(result);
        sb = new StringBuffer();
        while (vanillaHexMatcher.find()) {
            String hex = extractHex(vanillaHexMatcher.group(), "§x§");
            vanillaHexMatcher.appendReplacement(sb, "<#" + hex + ">");
        }
        vanillaHexMatcher.appendTail(sb);
        result = sb.toString();

        // === Шаг 6: Legacy &f, &c, &l... → <white>, <red>, <bold>... ===
        result = LEGACY_AMPERSAND.matcher(result).replaceAll(match -> {
            char code = match.group(1).charAt(0);
            return legacyCodeToMiniMessage(code);
        });

        // === Шаг 7: Ванильный §f, §c, §l... → <white>, <red>, <bold>... ===
        result = VANILLA_SECTION.matcher(result).replaceAll(match -> {
            char code = match.group(1).charAt(0);
            return legacyCodeToMiniMessage(code);
        });

        return result;
    }

    /**
     * Конвертирует CMI-подобный градиентный синтаксис:
     * {#FF5733>}текст{#000000<} → <gradient:#FF5733:#000000>текст</gradient>
     */
    private static String convertCmiGradient(String input) {
        Matcher startMatcher = CMI_GRADIENT_START.matcher(input);
        Matcher endMatcher = CMI_GRADIENT_END.matcher(input);

        if (!startMatcher.find()) return input;

        StringBuffer result = new StringBuffer();
        int lastEnd = 0;

        startMatcher.reset();

        while (startMatcher.find()) {
            String startColor = startMatcher.group(1);
            int startPos = startMatcher.start();
            int contentStart = startMatcher.end();

            if (endMatcher.find(contentStart)) {
                String endColor = endMatcher.group(1);
                int endPos = endMatcher.start();
                int endTagEnd = endMatcher.end();

                String content = input.substring(contentStart, endPos);

                result.append(input, lastEnd, startPos);
                result.append("<gradient:#").append(startColor).append(":#").append(endColor).append(">");
                result.append(content);
                result.append("</gradient>");

                lastEnd = endTagEnd;
            } else {
                break;
            }
        }

        if (lastEnd > 0) {
            result.append(input.substring(lastEnd));
            return result.toString();
        }

        return input;
    }

    // ──── Методы для теней ────

    /**
     * Генерирует MiniMessage shadow тег с учётом прозрачности.
     *
     * @param color   Цвет в формате #RRGGBB или #RRGGBBAA, или название цвета MiniMessage
     * @param offsetX Смещение по X
     * @param offsetY Смещение по Y
     * @param opacity Прозрачность 0.0-1.0 (приоритетнее альфы в color)
     * @return Открывающий тег &lt;shadow:...&gt;
     */
    public static String buildShadowTag(String color, int offsetX, int offsetY, double opacity) {
        String cleanColor = normalizeColor(color, opacity);

        if (opacity < 0.999) {
            return "<shadow:" + cleanColor + ":" + offsetX + ":" + offsetY + ":" + opacity + ">";
        }

        return "<shadow:" + cleanColor + ":" + offsetX + ":" + offsetY + ">";
    }

    /**
     * Нормализует цвет: добавляет альфа-канал если нужно.
     * #FF0000 + opacity 0.5 → #FF000080
     * #FF0000AA + opacity 1.0 → #FF0000AA
     * #FF0000 + opacity 1.0 → #FF0000
     * "red" → #FF5555
     */
    private static String normalizeColor(String color, double opacity) {
        if (color == null || color.isEmpty()) return "#000000";

        String hex = color.startsWith("#") ? color.substring(1) : color;

        // Конвертируем названия цветов MiniMessage в HEX
        hex = resolveNamedColor(hex);

        // Если уже 8 символов (RGBA) - оставляем как есть
        if (hex.length() == 8) {
            return "#" + hex.toUpperCase();
        }

        // Если 6 символов (RGB) и opacity < 1.0 - добавляем альфа
        if (hex.length() == 6 && opacity < 0.999) {
            int alpha = (int) Math.round(opacity * 255);
            String alphaHex = String.format("%02X", alpha);
            return "#" + hex.toUpperCase() + alphaHex;
        }

        // Если 6 символов и opacity == 1.0 - возвращаем как есть
        if (hex.length() == 6) {
            return "#" + hex.toUpperCase();
        }

        return "#000000";
    }

    /**
     * Разрешает названия цветов MiniMessage в HEX.
     */
    private static String resolveNamedColor(String color) {
        return switch (color.toLowerCase()) {
            case "black" -> "000000";
            case "dark_blue", "darkblue" -> "0000AA";
            case "dark_green", "darkgreen" -> "00AA00";
            case "dark_aqua", "darkaqua" -> "00AAAA";
            case "dark_red", "darkred" -> "AA0000";
            case "dark_purple", "darkpurple" -> "AA00AA";
            case "gold" -> "FFAA00";
            case "gray" -> "AAAAAA";
            case "dark_gray", "darkgray" -> "555555";
            case "blue" -> "5555FF";
            case "green" -> "55FF55";
            case "aqua" -> "55FFFF";
            case "red" -> "FF5555";
            case "light_purple", "lightpurple" -> "FF55FF";
            case "yellow" -> "FFFF55";
            case "white" -> "FFFFFF";
            default -> color;
        };
    }

    /**
     * Оборачивает текст в shadow тег с полной поддержкой прозрачности.
     */
    public static String wrapWithShadow(String text, String color, int offsetX, int offsetY, double opacity) {
        String openTag = buildShadowTag(color, offsetX, offsetY, opacity);
        return openTag + text + "</shadow>";
    }
}