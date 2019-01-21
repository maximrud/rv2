/**
 * Фигура на доске
 * @param {int} number Номер строки
 * @param {int} offset Смещение начала фигуры
 * @param {int} hand Цвет камней фигуры
 * @param {Pattern} pattern Шаблон фигуры
 */
function Figure(number, offset, hand, pattern) {

    // Номер строки
    this.number = number;

    // Смещение начала фигуры
    this.offset = offset;

    // Цвет камней фигуры
    this.hand = hand;

    // Шаблон фигуры
    this.pattern = pattern;
}

// Функция сравнения фигур
Figure.Comparator = function (hand) {

    return function (f1, f2) {
        return (f1.hand === hand ? f1.pattern.type : (f1.pattern.type + 2))
                - (f2.hand === hand ? f2.pattern.type : (f2.pattern.type + 2));
    };

};

// Поместить в коллекцию
Figure.prototype.addMoves = function (r, cols) {
    cols = cols || this.pattern.moves;
    for (var i = 0; i < cols.length; i++) {
        var square = Line.posSquare(this.number, this.offset + cols[i])
        if (r.indexOf(square) === -1)
            r.push(square);
    }
};

// Построить массив полей по относительным координатам
Figure.prototype.moves = function (cols) {
    var r = [];
    cols = cols || this.pattern.moves;
    this.addMoves(r, cols);
    return r;
};

// Занято ли поле фигурой
Figure.prototype.contains = function (square, cols) {
    cols = cols || this.pattern.moves;
    for (var i = 0; i < cols.length; i++) {
        if (square === Line.posSquare(this.number, this.offset + cols[i])) {
            return true;
        }
    }
    return false;
};

// Ходы усиления
Figure.prototype.gains = function () {
    return this.moves(this.pattern.gains);
};

// Ходы усиления
Figure.prototype.addGains = function (r) {
    this.addMoves(r, this.pattern.gains);
};

// Ходы закрытия
Figure.prototype.downs = function () {
    return this.moves(this.pattern.downs);
};

// Ходы закрытия
Figure.prototype.addDowns = function (r) {
    this.addMoves(r, this.pattern.downs);
};

// Ходы, ломающие фигуру
Figure.prototype.rifts = function () {
    return this.moves(this.pattern.rifts);
};

// Ходы, ломающие фигуру
Figure.prototype.addRifts = function (r) {
    this.addMoves(r, this.pattern.rifts);
};

// Проверить находится ли ход на линии
Figure.prototype.onSameLine = function (square) {
    return Line.onSameLine(this.number, square);
};

