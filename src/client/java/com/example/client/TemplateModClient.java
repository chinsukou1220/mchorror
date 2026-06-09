package com.example.client;

import com.example.TemplateMod;

import com.example.client.renderer.HorrorSteveRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class TemplateModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {

		EntityRendererRegistry.register(TemplateMod.HORROR_STEVE, HorrorSteveRenderer::new);
	}
}