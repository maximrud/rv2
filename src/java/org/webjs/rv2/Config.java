package org.webjs.rv2;

/**
 * Конфигурация расчета
 * @author rmr
 */
public class Config {
    
    public static Config defaults() {
        return new Config();
    }
   
    // Целевое максимальное число ходов
    public int computetarget = 35;

    // Глубина проверки последовательностей
    public int estimatedepth = 17;
    
    // Максимальное число позиций при проверке последовательностей
    public int estimatesize = 25000; 
    
    // Глубина расчета
    public int computedepth = 8;
    
    // Максимальное число позиций при расчете
    public int computesize = 2500000; 
    
    // Количество предложений при расчете
    public int computeedges = 15; //12

    // При расчете учитывать полный перебор
    public int computebrute = 0; //1

    public int computeweight = 1;
 
}
