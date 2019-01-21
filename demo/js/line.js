/**
 * Линия
 * @param {int} number Номер линии
 * @param {int} blen Количество черных камней или stroke смещение одного камня
 * @param {int} wlen Количество белых камней или offset цвет одного камня
 * @param {int} stroke Строка байт 01 - белые, 10 - черные
 * @param {int} inverse Инверсная строка байт
 */
function Line(number, blen, wlen, stroke, inverse) {

    // Номер линии
    this.number = number;

    // Количество черных камней
    this.blen = blen || 0;

    // Количество белых камней
    this.wlen = wlen || 0;

    // Строка байт 01 - белые, 10 - черные
    this.stroke = stroke || 0;

    // Инверсная строка байт 
    this.inverse = inverse || 0;

    if (typeof stroke === 'undefined' && typeof inverse === 'undefined' &&
            typeof blen !== 'undefined' && typeof wlen !== 'undefined') {
        // Создать линию с одним камнем
        var offset = blen;
        var hand = wlen;
        if (hand > 0) {
            this.blen = 1;
            this.wlen = 0;
        } else {
            this.blen = 0;
            this.wlen = 1;
        }
        var shift = offset << 1;
        this.stroke = 1 << (shift + hand);
        var right = (Line.lineLength(number) - 1) << 1;
        this.inverse = 1 << (right - shift + hand);

    }
}

// Установить камень в требуемую позицию на линии
Line.prototype.putStone = function (offset, hand) {
    var shift = offset << 1;
    if ((this.stroke & (3 << shift)) > 0) { // Ячейка уже занята
        throw new RangeError("Square already occupated");
    }
    var str = (this.stroke & (~(3 << shift))) | (1 << (shift + hand));
    var right = (Line.lineLength(this.number) - 1) << 1;
    var inv = (this.inverse & (~(3 << (right - shift)))) | (1 << (right - shift + hand));
    return hand > 0
            ? new Line(this.number, this.blen + 1, this.wlen, str, inv)
            : new Line(this.number, this.blen, this.wlen + 1, str, inv);
};

// Направление линии
Line.LEFT_RIGTH = 0;
Line.TOP_DOWN = 1;
Line.LTOP_RDOWN = 2;
Line.RTOP_LDOWN = 3;

// Получить цвет камня на поле. -1 если камня нет
Line.prototype.getStone = function (offset) {
    var shift = offset << 1;
    if ((this.stroke & (1 << shift)) > 0) {
        return 0;
    }
    if ((this.stroke & (1 << (shift + 1))) > 0) {
        return 1;
    }
    return -1;
};

// Убрать камень из позиции
Line.prototype.removeStone = function (offset, hand) {
    var shift = offset << 1;
    if ((this.stroke & (3 << shift)) !== (1 << (shift + hand))) { // В ячейке нет нужного камня
        throw new RangeError("No hand stone in the square");
    }
    var str = (this.stroke & (~(3 << shift)));
    var right = (Line.lineLength(this.number) - 1) << 1;
    var inv = (this.inverse & (~(3 << (right - shift))));
    return hand > 0
            ? new Line(this.number, this.blen - 1, this.wlen, str, inv)
            : new Line(this.number, this.blen, this.wlen - 1, str, inv);
};


// Идентификатор линии с учетом направления
Line.lineNumber = function (direction, square) {
    switch (direction) {
        case Line.LEFT_RIGTH:
            // Номер строки row
            return (direction << 5) | (square >> 4);
        case Line.TOP_DOWN:
            // Номер колонки col 
            return (direction << 5) | (square & 0xf);
        case Line.LTOP_RDOWN:
            // Номер диагонали из левого верхнего угла col - row + 14
            return (direction << 5) | ((square & 0xf) - (square >> 4) + 14);
        case Line.RTOP_LDOWN:
            // Номер диагонали из правого верхнего угла col + row
            return (direction << 5) | ((square & 0xf) + (square >> 4));
        default:
            return 0;
    }
};

// Проверка находится ли поле на линии по номеру
Line.onSameLine = function (number, square) {
    return number === Line.lineNumber(number >> 5, square);
};

// Длина строки
Line.lineLength = function (number) {
    var direction = number >> 5;
    switch (direction) {
        case Line.LTOP_RDOWN:
        case Line.RTOP_LDOWN:
            // 15 середина от нее влево вправо на один меньше
            var i = number & 0x1f;
            return i > 14 ? 29 - i : i + 1;
        default:
            return 15;
    }
};

// Индекс позиции на линии
Line.lineOffset = function (direction, square) {
    var col = square & 0xf;
    var row = square >> 4;
    switch (direction) {
        case Line.LEFT_RIGTH:
            // Номер колонки col
            return col;
        case Line.TOP_DOWN:
            // Номер строки row
            return row;
        case Line.LTOP_RDOWN:
            // До середины номер колонки col, после номер строки row
            return col > row ? row : col;
        case Line.RTOP_LDOWN:
            // До середины номер строки row, затем 14 - номер колонки col
            return col + row > 14 ? 14 - col : row;
        default:
            return 0;
    }
};

// Идентификатор поля на доске по номеру линии и расстоянию
Line.posSquare = function (number, offset) {
    var direction = number >> 5;
    var n = number & 0x1f;
    switch (direction) {
        case Line.LEFT_RIGTH:
            // По строкам
            return (n << 4) | offset;
        case Line.TOP_DOWN:
            // По колонкам
            return (offset << 4) | n;
        case Line.LTOP_RDOWN:
            // Диагональ из левого верхнего угла 
            return n > 14 ? (offset << 4) | (n + offset - 14)
                    : ((offset - n + 14) << 4) | offset;
        case Line.RTOP_LDOWN:
            // Диагональ из правого верхнего угла
            return n > 14 ? ((n + offset - 14) << 4) | (14 - offset)
                    : (offset << 4) | (n - offset);
        default:
            return 0;
    }
};

// Поиск выбранных фигур в строке
Line.prototype.findFigures = function (figures, type) {
    type = type || Pattern.ONE;
    // Длина линии
    var len = Line.lineLength(this.number);
    // Строка
    var stroke = this.stroke;
    // Число черных камней
    var bl = this.blen;
    // Число белых камней
    var wl = this.wlen;
    // Маска строки
    var probe = stroke;
    // Текущий номер камня
    var move = 0;
    while (probe > 0) {

        // По черным камням
        if ((probe & 2) > 0) {
            // По все шаблонам
            Pattern.SOLVER_PATTERNS.forEach(function (patterns) {
                patterns.some(function (pattern) {
                    // Левый край шаблона
                    var offset = move - pattern.move;
                    // Если шаблон соответствует запросу
                    if (pattern.type <= type
                            // Число камней в шаблоне не больше оставшихся
                            && bl >= pattern.moves.length
                            // Не выходит за границу доски
                            && offset >= 0 && offset + pattern.length <= len
                            // Нет черного каменя слева
                            && (offset > 0 ? (stroke >> ((offset - 1) << 1)) & 2 : 0) === 0
                            // Нет черного камня справа
                            && ((stroke >> ((offset + pattern.length) << 1)) & 2) === 0
                            // Соответствует проверочное значение
                            && ((stroke >> (offset << 1)) & pattern.mask) === pattern.black) {
                        // Добавить найденную фигуры и перейти к поиску от следующего камня
                        figures.push(new Figure(this.number, offset, 1, pattern));
                        return true;
                    }
                }, this);
            }, this);
            bl--;
        }

        // По белым камням
        if ((probe & 1) > 0) {
            // По все шаблонам
            Pattern.SOLVER_PATTERNS.forEach(function (patterns) {
                patterns.some(function (pattern) {
                    // Левый край шаблона
                    var offset = move - pattern.move;
                    // Если шаблон соответствует запросу
                    if (pattern.type <= type
                            // Число камней в шаблоне не больше оставшихся
                            && wl >= pattern.moves.length
                            // Не выходит за границу доски
                            && offset >= 0 && offset + pattern.length <= len
                            // Нет белого каменя слева
                            && (offset > 0 ? (stroke >> ((offset - 1) << 1)) & 1 : 0) === 0
                            // Нет белого камня справа
                            && ((stroke >> ((offset + pattern.length) << 1)) & 1) === 0
                            // Соответствует проверочное значение
                            && ((stroke >> (offset << 1)) & pattern.mask) === pattern.white) {
                        // Добавить найденную фигуры и перейти к поиску от следующего камня
                        figures.push(new Figure(this.number, offset, 0, pattern));
                        return true;
                    }
                }, this);
            }, this);
            wl--;
        }

        probe >>= 2;
        move++;
    }
};

Line.prototype.compareTo = function (e) {
    return this.number < e.number ? -1 : this.number > e.number ? 1
            : this.stroke < e.stroke ? -1 : this.stroke > e.stroke ? 1 : 0;
};

Line.Comparator = function (line1, line2) {
    return line1.number < line2.number ? -1 : line1.number > line2.number ? 1
            : line1.stroke < line2.stroke ? -1 : line1.stroke > line2.stroke ? 1 : 0;
};

Line.prototype.hashCode = function () {
    var hash = 3;
    hash = 89 * hash + this.stroke;
    hash = 89 * hash + this.number;
    return hash;
};

Line.prototype.equals = function (obj) {
    if (this === obj) {
        return true;
    }
    if (obj === null) {
        return false;
    }
    return this.stroke === obj.stroke
            && this.number === obj.number;
};
