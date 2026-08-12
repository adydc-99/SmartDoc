package com.smartdoc.ai;

import java.time.*;

public final class DailyAiQuota {
    private final Clock clock; private LocalDate date; private int used;
    public DailyAiQuota(Clock clock){this.clock=clock;this.date=today();}
    public synchronized void consume(int limit){rollover();if(used>=limit)throw new AiQuotaExceededException();used++;}
    public synchronized int used(){rollover();return used;}
    private LocalDate today(){return LocalDate.now(clock);}
    private void rollover(){LocalDate now=today();if(!now.equals(date)){date=now;used=0;}}
}
