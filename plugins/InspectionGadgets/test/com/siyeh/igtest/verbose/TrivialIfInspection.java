package com.siyeh.igtest.verbose;

public class TrivialIfInspection
{
    public boolean foo()
    {
        boolean x;
        if(bar())
        {
            x = true;
        }
        else
        {
            x = false;
        }
        System.out.println("x = " + x);
        if(bar())
        {
            x = false;
        }
        else
        {
            x = true;
        }
        System.out.println("x = " + x);

        if(bar())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private boolean bar()
    {
        return true;
    }
}
