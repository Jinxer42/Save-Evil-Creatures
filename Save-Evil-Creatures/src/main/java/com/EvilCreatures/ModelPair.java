package com.EvilCreatures;

import net.runelite.api.NPC;
import net.runelite.api.RuneLiteObject;

public class ModelPair {

    private NPC gnome;
    private RuneLiteObject evilCreatureHead;
    private RuneLiteObject evilCreatureBody;
    protected ModelPair(NPC inNPC, RuneLiteObject inHead, RuneLiteObject inBody)
    {
        setNPC(inNPC);
        setHead(inHead);
        setBody(inBody);
    }

    private void setNPC(NPC inNPC)
    {
        gnome = inNPC;
    }
    private void setHead(RuneLiteObject inHead)
    {
        evilCreatureHead=inHead;
    }
    private void setBody(RuneLiteObject inBody)
    {
        evilCreatureBody=inBody;
    }

    protected NPC getNPC()
    {
        return gnome;
    }
    protected RuneLiteObject getHead()
    {
        return evilCreatureHead;
    }
    protected RuneLiteObject getBody()
    {
        return evilCreatureBody;
    }
}
