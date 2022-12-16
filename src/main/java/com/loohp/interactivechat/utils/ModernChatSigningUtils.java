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

import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.loohp.interactivechat.InteractiveChat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public class ModernChatSigningUtils {

    private static Class<?> nmsIChatBaseComponent;
    private static Class<?> nmsChatMessageTypeBClass;
    private static Class<?> nmsPlayerChatMessageClass;
    private static Class<?> nmsArgumentSignaturesClass;
    private static Class<?> nmsChatMessageContentClass;
    private static Constructor<?> nmsChatMessageContentConstructor;
    private static Method nmsArgumentSignaturesEntries;
    private static Field nmsChatMessageTypeBChatTypeField;
    private static Method nmsChatMessageWithChatMessageContentMethod;
    private static Method nmsPlayerChatMessageFromStringMethod;
    private static Field nmsPlayerChatMessageUnsignedContentField;
    private static Method nmsPlayerChatMessageWithUnsignedContentMethod;
    private static Field nmsPlayerChatMessageSignedBodyField;
    private static Field nmsSignedMessageBodyChatMessageContentField;
    private static Field nmsChatMessageContentContentField;
    private static Class<?> nmsSignedMessageBodyAClass;
    private static Field nmsSignedMessageBodyAContentField;

    static {
        if (InteractiveChat.hasChatSigning()) {
            try {
                nmsIChatBaseComponent = NMSUtils.getNMSClass("net.minecraft.network.chat.IChatBaseComponent");
                nmsChatMessageTypeBClass = NMSUtils.getNMSClass("net.minecraft.network.chat.ChatMessageType$b");
                nmsPlayerChatMessageClass = NMSUtils.getNMSClass("net.minecraft.network.chat.PlayerChatMessage");
                nmsArgumentSignaturesClass = NMSUtils.getNMSClass("net.minecraft.commands.arguments.ArgumentSignatures");
                if (InteractiveChat.version.isOlderThan(MCVersion.V1_19_3)) {
                    nmsChatMessageContentClass = NMSUtils.getNMSClass("net.minecraft.network.chat.ChatMessageContent");
                    nmsChatMessageContentConstructor = nmsChatMessageContentClass.getConstructor(String.class);
                    nmsChatMessageWithChatMessageContentMethod = nmsPlayerChatMessageClass.getMethod("a", nmsChatMessageContentClass);
                } else {
                    nmsPlayerChatMessageFromStringMethod = nmsPlayerChatMessageClass.getMethod("a", String.class);
                }
                nmsArgumentSignaturesEntries = nmsArgumentSignaturesClass.getMethod("a");
                nmsChatMessageTypeBChatTypeField = nmsChatMessageTypeBClass.getDeclaredField("a");
                nmsPlayerChatMessageUnsignedContentField = nmsPlayerChatMessageClass.getDeclaredField("f");
                nmsPlayerChatMessageWithUnsignedContentMethod = nmsPlayerChatMessageClass.getMethod("a", nmsIChatBaseComponent);
                if (InteractiveChat.version.isNewerOrEqualTo(MCVersion.V1_19_3)) {
                    nmsPlayerChatMessageSignedBodyField = nmsPlayerChatMessageClass.getDeclaredField("f");
                } else {
                    nmsPlayerChatMessageSignedBodyField = nmsPlayerChatMessageClass.getDeclaredField("e");
                }
                nmsSignedMessageBodyChatMessageContentField = nmsPlayerChatMessageSignedBodyField.getType().getDeclaredField("b");
                if (InteractiveChat.version.isOlderThan(MCVersion.V1_19_3)) {
                    nmsChatMessageContentContentField = nmsSignedMessageBodyChatMessageContentField.getType().getDeclaredField("b");
                }
                if (InteractiveChat.version.isNewerOrEqualTo(MCVersion.V1_19_3)) {
                    nmsSignedMessageBodyAClass = NMSUtils.getNMSClass("net.minecraft.network.chat.SignedMessageBody$a");
                    nmsSignedMessageBodyAContentField = nmsSignedMessageBodyAClass.getDeclaredField("a");
                }
            } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        } else {
            throw new IllegalStateException("This class should only be used on version 1.19.1 or above");
        }
    }

    public static int getChatMessageType(Object chatMessageTypeB) {
        nmsChatMessageTypeBChatTypeField.setAccessible(true);
        try {
            return nmsChatMessageTypeBChatTypeField.getInt(chatMessageTypeB);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Object getNMSPlayerChatMessage(String message) {
        try {
            if (InteractiveChat.version.isNewerOrEqualTo(MCVersion.V1_19_3)) {
                return nmsPlayerChatMessageFromStringMethod.invoke(null, message);
            } else {
                Object nmsChatMessageContentObject = nmsChatMessageContentConstructor.newInstance(message);
                return nmsChatMessageWithChatMessageContentMethod.invoke(null, nmsChatMessageContentObject);
            }
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Optional<?> getUnsignedContent(Object playerChatMessage) {
        nmsPlayerChatMessageUnsignedContentField.setAccessible(true);
        try {
            return (Optional<?>) nmsPlayerChatMessageUnsignedContentField.get(playerChatMessage);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Object getSignedContent(Object playerChatMessage) {
        nmsPlayerChatMessageSignedBodyField.setAccessible(true);
        nmsSignedMessageBodyChatMessageContentField.setAccessible(true);
        nmsChatMessageContentContentField.setAccessible(true);
        try {
            Object signedMessageBody = nmsPlayerChatMessageSignedBodyField.get(playerChatMessage);
            Object chatMessageContent = nmsSignedMessageBodyChatMessageContentField.get(signedMessageBody);
            if (chatMessageContent instanceof String) {
                return chatMessageContent;
            }
            return nmsChatMessageContentContentField.get(chatMessageContent);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Object withUnsignedContent(Object playerChatMessage, Object unsignedContent) {
        try {
            return nmsPlayerChatMessageWithUnsignedContentMethod.invoke(playerChatMessage, unsignedContent);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return WrappedChatComponent.fromText("").getHandle();
    }

    public static boolean isArgumentSignatureClass(Object instance) {
        return nmsArgumentSignaturesClass.isInstance(instance);
    }

    public static List<?> getArgumentSignatureEntries(Object argumentSignatures) {
        try {
            return (List<?>) nmsArgumentSignaturesEntries.invoke(argumentSignatures);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getSignedMessageBodyAContent(Object signedMessageBodyA) {
        try {
            nmsSignedMessageBodyAContentField.setAccessible(true);
            return (String) nmsSignedMessageBodyAContentField.get(signedMessageBodyA);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

}
