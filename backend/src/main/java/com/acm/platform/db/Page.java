package com.acm.platform.db;
import java.util.List;
public record Page<T>(List<T> items,int page,int pages,int start,int end,long total,int size) {
 public static int size(int value){return Math.max(1,Math.min(value,100));}
 public static int pages(long total,int size){return (int)Math.max(1,(total+size-1)/size);}
 public static int current(int page,long total,int size){return Math.max(1,Math.min(page,pages(total,size)));}
 public static <T> Page<T> of(List<T> items,int page,int size,long total){
  int start=total==0?0:(page-1)*size+1;
  return new Page<>(items,page,pages(total,size),start,(int)Math.min((long)page*size,total),total,size);
 }
}
