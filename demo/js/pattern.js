/**
 * Шаблоны фигур
 * @param {int} type Тип фигуры
 * @param {string} template Шаблон фигуры
 * @param {int} rating Количество очков фигуры
 */
function Pattern(type, template, rating) {

    // Тип фигуры: 
    //      0 - пятерка, 
    //      1 - открытая четверка, 
    //      2 - четверка,
    //      3 - открытая тройка, 
    //      4 - тройка, 
    //      5 - открыая двойка
    this.type = type;

    // Количество очков фигуры
    this.rating = rating;

    // Битовая строка для сравнения
    var s = 0;

    // Ширина шаблона
    var length = this.length = template.length;

    // Маска по длине фигуры
    var mask = this.mask = (1 << (length << 1)) - 1;

    for (var i = length - 1; i >= 0; --i) {
        if (template.charAt(i) === 'x') {
            s = s | 1;
        }
        s = s << 2;
    }
    s = s >> 2;

    // Сравнение для белых камней
    this.white = s;

    // Сравнение для черных камней
    this.black = s << 1;

    // Рассчитать cмещение полей на линии
    var moves = [];
    var gains = [];
    var downs = [];
    var rifts = [];
    for (var i = 0; i < length; i++) {
        var ch = template.charAt(i);
        switch (ch) {
            case 'x':
                moves.push(i);
                break;
            case '-':
                gains.push(i);
                rifts.push(i);
                break;
            case '+':
                gains.push(i);
                downs.push(i);
                rifts.push(i);
                break;
            case '!':
                downs.push(i);
                rifts.push(i);
                break;
            default:
                rifts.push(i);
                break;
        }
    }

    // Растояние от левого края до камней 
    this.moves = moves;

    // Расстояние от левого края до левого камня
    this.move = moves[0];

    // Растояние от левого края усиливающих позицию ходов 
    this.gains = gains;

    // Растояние от левой края закрывающих позицию ходов
    this.downs = downs;

    // Растояние от левой края ходов влюящих на позицию
    this.rifts = rifts;
}

// Тип фигуры: 
//      0 - пятерка, 
//      1 - открытая четверка, 
//      2 - четверка,
//      3 - открытая тройка, 
//      4 - тройка, 
//      5 - открыая двойка
//      6 - открыая единица
Pattern.FIVE = 0;
Pattern.OPEN_FOUR = 3;
Pattern.FOUR = 4;
Pattern.OPEN_THREE = 7;
Pattern.THREE = 10;
Pattern.OPEN_TWO = 11;
Pattern.TWO = 14;
Pattern.ONE = 15;

// Последовательность сканирования шаблонов
// Шаблон содержит следующие символы:
//      x заполненное поле
//      + поле возможной атака и закрывающее атаку оппонента
//      - поле возможной атаки и не закрыващее атаку оппонента
//      ! поле безусловно закрывающее атаку оппонента
//      . поле отступа, влияет на развитие атаки
Pattern.SOLVER_PATTERNS = [
    [
        // Пятерка: XXXXX*
        new Pattern(Pattern.FIVE, "xxxxx", 0)
    ], [
        // Открытая четверка: *XXXX*
        new Pattern(Pattern.OPEN_FOUR, "+xxxx+", 0),
        // Четверки: XXXX*, XXX*X, XX*XX
        new Pattern(Pattern.FOUR, "xxxx+", 5),
        new Pattern(Pattern.FOUR, "+xxxx", 5),
        new Pattern(Pattern.FOUR, "xxx+x", 5),
        new Pattern(Pattern.FOUR, "x+xxx", 5),
        new Pattern(Pattern.FOUR, "xx+xx", 5)
    ], [
        // Открытые тройки: **XXX**, *XXX**, *X*XX*
        new Pattern(Pattern.OPEN_THREE, ".+xxx+.", 4), // 4 points
        new Pattern(Pattern.OPEN_THREE, "!xxx+!", 3), // 3 points 
        new Pattern(Pattern.OPEN_THREE, "!+xxx!", 3), // 3 points 
        new Pattern(Pattern.OPEN_THREE, "!xx+x!", 2), // 2 points
        new Pattern(Pattern.OPEN_THREE, "!x+xx!", 2), // 2 points
        // Тройки: XXX**, X*XX*, XX*X*, XX**X, X**XX
        new Pattern(Pattern.THREE, "xxx++", 1),
        new Pattern(Pattern.THREE, "++xxx", 1),
        new Pattern(Pattern.THREE, "+xxx+", 1),
        new Pattern(Pattern.THREE, "x+xx+", 1),
        new Pattern(Pattern.THREE, "+xx+x", 1),
        new Pattern(Pattern.THREE, "xx+x+", 1),
        new Pattern(Pattern.THREE, "+x+xx", 1),
        new Pattern(Pattern.THREE, "xx++x", 1),
        new Pattern(Pattern.THREE, "x++xx", 1),
        new Pattern(Pattern.THREE, "x+x+x", 1)
    ], [
        // Открытые двойки: ***XX***, **XX***, *XX***, **XX**, *X*X**, *X**X*
        new Pattern(Pattern.OPEN_TWO, ".-+xx+-.", 2), // 2 points   .-x--.. .x-... 
        new Pattern(Pattern.OPEN_TWO, ".+xx+-.", 2), // 2 points
        new Pattern(Pattern.OPEN_TWO, ".-+xx+.", 2), // 2 points
        new Pattern(Pattern.OPEN_TWO, ".+xx+.", 2), // 2 points
        new Pattern(Pattern.OPEN_TWO, "!xx+-.", 2), // 2 points
        new Pattern(Pattern.OPEN_TWO, ".-+xx!", 2), // 2 points
        new Pattern(Pattern.OPEN_TWO, ".+x+x+.", 1), // 1 points
        new Pattern(Pattern.OPEN_TWO, "!x+x+.", 1),
        new Pattern(Pattern.OPEN_TWO, ".+x+x!", 1),
        new Pattern(Pattern.OPEN_TWO, "!x++x!", 1),
        // Двойки: XX***, *XX**, X*X**, *X*X*, X**X*, X***X
        new Pattern(Pattern.TWO, "xx+--", 0),
        new Pattern(Pattern.TWO, "--+xx", 0),
        new Pattern(Pattern.TWO, "+xx+-", 0),
        new Pattern(Pattern.TWO, "-+xx+", 0),
        new Pattern(Pattern.TWO, "x+x+-", 0),
        new Pattern(Pattern.TWO, "-+x+x", 0),
        new Pattern(Pattern.TWO, "+x+x+", 0),
        new Pattern(Pattern.TWO, "x++x+", 0),
        new Pattern(Pattern.TWO, "+x++x", 0),
        new Pattern(Pattern.TWO, "x+++x", 0)
    ], [
        // Перспективные единицы: ****X****, ***X****, ***X***, **X****, **X***, *X****
        new Pattern(Pattern.ONE, "..--x--..", 0),
        new Pattern(Pattern.ONE, "..-x--..", 0),
        new Pattern(Pattern.ONE, "..--x-..", 0),
        new Pattern(Pattern.ONE, "..-x-..", 0),
        new Pattern(Pattern.ONE, ".-x--..", 0),
        new Pattern(Pattern.ONE, ".--x-..", 0),
        new Pattern(Pattern.ONE, ".-x-..", 0),
        new Pattern(Pattern.ONE, "..-x-.", 0),
        new Pattern(Pattern.ONE, ".x-...", 0),
        new Pattern(Pattern.ONE, "...-x.", 0)
    ]
];
