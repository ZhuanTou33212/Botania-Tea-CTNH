package com.qianxin.botaniatea.content;

import net.minecraft.resources.ResourceLocation;
import java.util.Set;

public record ItemElementProfile(ResourceLocation itemId, ElementVector vector, Set<String> categories,
                                 String status, String source, String warning) {}
