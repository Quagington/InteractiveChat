/*
 * This file is part of InteractiveChat.
 *
 * Copyright (C) 2022. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2022. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.interactivechat.utils;

import com.loohp.interactivechat.objectholders.Either;
import com.loohp.interactivechat.objectholders.ValuePairs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComponentReplacing {

    public static final String ESCAPE_PREPEND_PATTERN = "(?:(?<=\\\\)(\\\\)|(?<!\\\\))";
    public static final String ESCAPE_PLACEHOLDER_PATTERN = "\\\\(%s)";

    public static Component replace(Component component, String regex, Component replace) {
        return replace(component, regex, false, groups -> replace);
    }

    public static Component replace(Component component, String regex, boolean escaping, Component replace) {
        return replace(component, regex, escaping, groups -> replace);
    }

    public static Component replace(Component component, String regex, Function<ComponentMatchResult, Component> replaceFunction) {
        return replace(component, regex, false, replaceFunction);
    }

    public static Component replace(Component component, String regex, boolean escaping, Function<ComponentMatchResult, Component> replaceFunction) {
        return replace(component, regex, escaping, (result, replaced) -> replaceFunction.apply(result));
    }

    public static Component replace(Component component, String regex, boolean escaping, BiFunction<ComponentMatchResult, List<Component>, Component> replaceFunction) {
        String regexOriginal = regex;
        if (escaping) {
            regex = ESCAPE_PREPEND_PATTERN + regex;
        }
        component = ComponentFlattening.flatten(component);
        List<Component> children = new ArrayList<>(component.children());
        for (int i = 0; i < children.size(); i++) {
            Component child = children.get(i);
            if (child instanceof TranslatableComponent) {
                TranslatableComponent translatable = (TranslatableComponent) child;
                List<Component> args = new ArrayList<>(translatable.args());
                args.replaceAll(c -> replace(c, regexOriginal, escaping, replaceFunction));
                translatable = translatable.args(args);
                children.set(i, translatable);
            }
        }
        component = component.children(children);

        List<Either<ValuePairs<String, List<Component>>, Component>> sections = breakdown(component);
        Pattern pattern = Pattern.compile(regex);
        children = new ArrayList<>();
        for (Either<ValuePairs<String, List<Component>>, Component> either : sections) {
            if (either.isRight()) {
                children.add(either.getRight());
            } else {
                ValuePairs<String, List<Component>> pair = either.getLeft();
                List<Component> componentCharacters = pair.getSecond();
                Matcher matcher = pattern.matcher(pair.getFirst());
                int lastEnd = 0;
                while (matcher.find()) {
                    int start = matcher.start();
                    int end = matcher.end();
                    List<Component> componentGroup = Collections.unmodifiableList(componentCharacters.subList(start, end));
                    int originalLength = componentGroup.size();
                    Component result = replaceFunction.apply(new ComponentMatchResult(matcher, componentCharacters), componentGroup);
                    children.addAll(componentCharacters.subList(lastEnd, start));
                    children.add(result);
                    lastEnd = end;
                }
                children.addAll(componentCharacters.subList(lastEnd, componentCharacters.size()));
            }
        }

        component = ComponentCompacting.optimize(component.children(children));

        if (escaping) {
            component = replace(component, ESCAPE_PLACEHOLDER_PATTERN.replace("%s", regexOriginal), false, (result, replaced) -> result.groupComponent(1));
        }

        return component;
    }

    private static List<Either<ValuePairs<String, List<Component>>, Component>> breakdown(Component component) {
        List<Either<ValuePairs<String, List<Component>>, Component>> result = new ArrayList<>();
        Component flatten = ComponentFlattening.flatten(component);
        StringBuilder sb = new StringBuilder();
        List<Component> components = new ArrayList<>();
        for (Component c : flatten.children()) {
            if (c instanceof TextComponent) {
                TextComponent textComponent = (TextComponent) c;
                String content = textComponent.content();
                if (!content.isEmpty()) {
                    for (int i = 0; i < content.length();) {
                        int codePoint = content.codePointAt(i);
                        String character = new String(Character.toChars(codePoint));
                        i += character.length();
                        components.add(textComponent.content(character));
                        sb.append(character);
                    }
                }
            } else {
                if (!components.isEmpty()) {
                    result.add(Either.left(new ValuePairs<>(sb.toString(), components)));
                    sb = new StringBuilder();
                    components = new ArrayList<>();
                }
                result.add(Either.right(c));
            }
        }
        if (!components.isEmpty()) {
            result.add(Either.left(new ValuePairs<>(sb.toString(), components)));
        }
        return result;
    }

    public static final class ComponentMatchResult implements MatchResult {

        private final MatchResult backingResult;
        private final List<Component> componentCharacters;

        public ComponentMatchResult(MatchResult backingResult, List<Component> componentCharacters) {
            this.backingResult = backingResult;
            this.componentCharacters = componentCharacters;
        }

        @Override
        public int start() {
            return backingResult.start();
        }

        @Override
        public int start(int group) {
            return backingResult.start(group);
        }

        @Override
        public int end() {
            return backingResult.end();
        }

        @Override
        public int end(int group) {
            return backingResult.end(group);
        }

        @Override
        public String group() {
            return backingResult.group();
        }

        @Override
        public String group(int group) {
            return backingResult.group(group);
        }

        public Component groupComponent() {
            int start = backingResult.start();
            int end = backingResult.end();
            return ComponentCompacting.optimize(Component.empty().children(componentCharacters.subList(start, end)));
        }

        public Component groupComponent(int group) {
            int start = backingResult.start(group);
            int end = backingResult.end(group);
            return ComponentCompacting.optimize(Component.empty().children(componentCharacters.subList(start, end)));
        }

        @Override
        public int groupCount() {
            return backingResult.groupCount();
        }

    }

}
