package com.siyeh.igtest.verbose;

public class UnnecessaryReturnInspection
{

    public UnnecessaryReturnInspection()
    {
        return;
    }

    public void foo()
    {
        return;
    }

    public void bar()
    {
        if(true)
        {
            return;
        }
    }

}