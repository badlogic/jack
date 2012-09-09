package com.badlogic.jack;

import java.util.Arrays;

public class Sort {
	int[] feld = new int[5];
	
	public static void main(String[] args) {
		Sort sort = new Sort(new int[] { 14, 16, 18,20,22 });
		sort.insertSorted(15);
		sort.out();
	}
	
	public Sort(int[] feld) {
		this.feld = feld;
	}
	
	public void out() {
		System.out.println(Arrays.toString(feld));
	}
	
	public void insertSorted(int zahl)
    {
        int index = 0;
        
        for(int i = 0; i < feld.length; i++)
        {
            if(feld[i] > zahl)
            {
                index = i;
                break;
            }
        }
        
        for(int i = feld.length-1; i > index; i--)
        {
            feld[i] = feld[i-1];
        }
        
        feld[index] = zahl;
    }
}
