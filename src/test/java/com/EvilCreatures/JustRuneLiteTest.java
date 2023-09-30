package com.EvilCreatures;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class JustRuneLiteTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(EvilCreatureModelSwap.class);
		RuneLite.main(args);
	}
}