package com.smartdoc.ai;

import java.time.*; import java.util.*;

public final class DailyAiQuota {
    private final Clock clock; private LocalDate date; private int used; private final Map<Long,Integer> byUser=new HashMap<>();
    public DailyAiQuota(Clock clock){this.clock=clock;this.date=today();}
    public synchronized void consume(int limit){consume(0L,limit);}
    public synchronized void consume(long userId,int limit){rollover();int count=byUser.getOrDefault(userId,0);if(count>=limit)throw new AiQuotaExceededException();byUser.put(userId,count+1);if(userId==0L)used=count+1;}
    public synchronized int used(){return used(0L);} public synchronized int used(long userId){rollover();return byUser.getOrDefault(userId,0);}
    private LocalDate today(){return LocalDate.now(clock);}
    private void rollover(){LocalDate now=today();if(!now.equals(date)){date=now;used=0;byUser.clear();}}
}
