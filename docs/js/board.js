/**
 * Управление партией на доске
 * @param {Element} el Элемент управления
 */
function Board(el) {
    this.el = el;
    this.draw();
    this.start();
    var self = this;
    el.addEventListener('click', function (event) {
        // Кликать во время расчета нельзя
        if (self.calculating) {
            return;
        }
        var target = event.target;
        // Ход на свободное поле
        if (target.className === 'freecell') {
            self.move(target.dataset.col + target.parentElement.dataset.row);
        }
        // Нажатие кнопки
        if (target.dataset.action) {
            self[target.dataset.action].call(self);
        }
        // Включение/выключение автохода
        if (target.id === 'automove') {
            self.start();
        }
        // Включение/выключение подсказки
        if (target.id === 'showhelp') {
            self.help();
        }
    });
    this.load();
}

// Начальная инициализация
Board.prototype.start = async function () {
    this.record = ['h8'];
    this.attacker = Layout.BLACK;
    await this.find();
    this.fill();
};

// Нарисовать доску
Board.prototype.draw = function () {
    var desk = this.el.querySelector('table.desk');
    var s = '<tbody><tr><th></th>';
    var a = 'a'.charCodeAt(0);
    for (var c = 0; c < 15; c++) {
        var ch = String.fromCharCode(a + c);
        s += '<th>' + ch + '</th>';
    }
    s += '<th></th></tr>';
    for (var r = 15; r >= 1; --r) {
        s += '<tr data-row="' + r + '"><th>' + r + '</th>';
        for (var c = 0; c < 15; c++) {
            var ch = String.fromCharCode(a + c);
            s += '<td class="freecell" data-col="' + ch + '">&nbsp;</td>';
        }
        s += '<th>' + r + '</th></tr>';
    }
    s += '<tr><th></th>';
    for (var c = 0; c < 15; c++) {
        var ch = String.fromCharCode(a + c);
        s += '<th>' + ch + '</th>';
    }
    s += '<th></th></tr></tbody>';
    desk.innerHTML = s;
};

// Установить камень
Board.prototype.put = function (coord, label, color) {
    var cell = this.el.querySelector('table.desk > tbody > tr[data-row="'
            + coord.substring(1) + '"] > td[data-col="'
            + coord.charAt(0) + '"]');
    if (label) {
        cell.innerHTML = label;
    }
    if (color) {
        cell.className = color;
    }
};

// Убрать камень
Board.prototype.remove = function (coord) {
    var cell = coord.innerHTML ? coord :
            this.el.querySelector('table.desk > tbody > tr[data-row="'
                    + coord.substring(1) + '"] > td[data-col="'
                    + coord.charAt(0) + '"]');
    cell.innerHTML = '&nbsp;';
    cell.className = 'freecell';
};

// Расставить камни
Board.prototype.fill = function () {
    // Очистить доску
    var cells = this.el.querySelectorAll('table.desk > tbody > tr > td');
    for (var i = 0; i < cells.length; ++i) {
        this.remove(cells[i]);
    }
    // Нарисовать камни на доске
    this.record.forEach(function (coord, index) {
        this.put(coord, '' + (index + 1), index % 2 ? 'white' : 'black');
    }, this);
    // Нарисовать предложение по следующим ходам
    this.help();
    // Установить цвет текущего хода
    this.hand();
};

// Нарисовать предложение по следующим ходам
Board.prototype.help = function () {
    var cells = this.el.querySelectorAll('table.desk > tbody > tr > td.freecell');
    for (var i = 0; i < cells.length; ++i) {
        cells[i].innerHTML = '&nbsp;';
    }
    var help = this.el.querySelector('#showhelp').checked;
    var count = this.count();
    if (help && typeof this.current === 'object' && count > 0
            && this.record.length >= 2) {
        for (var coord in this.current) {
            if (coord !== 'count') {
                var next = this.current[coord];
                var c = Board.count(next);
                if (count === 225 || c === 225 || c === count - 1) {
                    this.put(this.transCoord(coord), '&diams;');
                }
            }
        }
    }
};

// Зафикировать выигрышь
Board.prototype.checkwin = function () {
    if (this.current.count === 1 && this.current.coords) {
        // Остановить партию и пометить выигрышные поля на доске
        var desk = this.el.querySelector('table.desk');
        desk.className = 'desk';
        var cells = this.el.querySelectorAll('table.desk > tbody > tr > td');
        for (var i = 0; i < cells.length; ++i) {
            cells[i].classList.remove('freecell');
        }
        this.current.coords.forEach(function (coord) {
            var cell = this.el.querySelector('table.desk > tbody > tr[data-row="'
                    + coord.substring(1) + '"] > td[data-col="'
                    + coord.charAt(0) + '"]');
            cell.classList.add('font-weight-bold');
        }, this);
    }
};

// Установить цвет текущего хода
Board.prototype.hand = function () {
    var color = this.record.length % 2 ? 'white' : 'black';
    var desk = this.el.querySelector('table.desk');
    desk.className = 'desk ' + color;
    var info = this.el.querySelector('table.info');
    var move = info.querySelector('[data-info="move"]');
    var left = info.querySelector('[data-info="left"]');
    var total = info.querySelector('[data-info="total"]');
    var count = this.count();
    move.className = color;
    move.innerHTML = count > 0 ? this.record.length + 1 : 'X';
    if (this.current && count < 225) {
        left.innerHTML = count;
        total.innerHTML = count + this.record.length;
    } else {
        left.innerHTML = '';
        total.innerHTML = '';
    }
};

// Сделать ход
Board.prototype.move = async function (coord) {
    this.calculating = true;
    var index = this.record.length;
    var color = index % 2 ? 'white' : 'black';
    this.record.push(coord);
    this.put(coord, '' + (index + 1), color);
    await this.find();
    this.help();
    this.hand();
    await this.auto();
    delete this.calculating;
};

// Автоматический ход черных/белых
Board.prototype.auto = function () {
    var self = this;
    return new Promise(function (resolve) {
        setTimeout(async function () {
            var auto = self.el.querySelector('#automove').checked
                    && (self.record.length % 2 === 0
                            ? self.attacker === Layout.BLACK
                            : self.attacker === Layout.WHITE);
            var count = self.count();
            if (auto && typeof self.current === 'object' && count > 0) {
                // Выбрать подходящие ходы
                var coords = [];
                for (var coord in self.current) {
                    if (coord !== 'count') {
                        var next = self.current[coord];
                        var c = Board.count(next);
                        if (count === 225 || c === 225 || c === count - 1) {
                            coords.push(coord);
                        }
                    }
                }
                // Если 2-й ход - выбрать случайное направление
                if (self.record.length === 1) {
                    self.transCode = Math.floor(Math.random() * 8);
                }
                // Выбрать случайный ход
                await self.move(self.transCoord(
                        coords[Math.floor(Math.random() * coords.length)]));
            }
            resolve();
        }, 10);
    });
};

// Отменить ход
Board.prototype.back = async function () {
    this.calculating = true;
    if (this.record.length > 1) {
        var waswin = this.current.count === 1;
        var coord = this.record.pop();
        this.remove(coord);
        await this.find();
        this.help();
        this.hand();
        if (waswin) {
            this.fill();
        }
        // Убираем ход белых/черных
        var auto = this.el.querySelector('#automove').checked
                && (this.record.length % 2 === 0 ?
                        this.attacker === Layout.BLACK : this.attacker === Layout.WHITE);
        if (auto) {
            await this.back();
            await this.auto();
        }
    }
    delete this.calculating;
};

// Загрузить решение 
Board.prototype.load = function () {
    var xobj = new XMLHttpRequest(), self = this;
    xobj.overrideMimeType("application/json");
    xobj.open('GET', 'data/gomoku.json', true);
    self.calculating = true;
    xobj.onreadystatechange = async function () {
        if (xobj.readyState === 4) {
            if (xobj.status === 200) {
                self.solution = JSON.parse(xobj.responseText);
                Board.fillCount(self.solution);
                await self.find();
                self.hand();
            }
            self.hideProgress();
            delete self.calculating;
        }
    };
    xobj.send(null);
    this.showProgress();
};

// Получить следующие ходы
Board.prototype.find = async function () {
    // Найти текущую позицию
    if (this.solution) {
        this.current = this.solution;
        this.transCode = 0;
        var need;
        // Расчет текущего решения
        this.record.forEach(function (coord, index) {
            if (index === 1) {
                // Только на 2-м ходе
                this.transCode = Board.coordTransCode(coord);
            }
            coord = this.transCoord(coord, true);
            var next = this.current[coord];
            if (typeof next !== 'object') {
                need = true;
                // Преобразовать в объект
                next = typeof next !== 'undefined' ? {count: next} : {};
                this.current[coord] = next;
            }
            this.current = next;
        }, this);
        // Рассчитать продолжение
        if (need) {
            await this.estimateProgress();
        }
        // Проверить на завершение
        this.checkwin();
    }
};

// Расчет с отображением строки прогресса
Board.prototype.estimateProgress = function () {
    var self = this;
    return new Promise(function (resolve) {
        self.showProgress();
        setTimeout(async function () {
            await self.estimate();
            self.hideProgress();
            resolve();
        }, 10);
    });
};

// Расчет обязательных ходов
Board.prototype.estimate = async function () {
    var moves = [];
    // Перевести в таблицу координа решения
    this.record.forEach(function (coord) {
        moves.push(Layout.transSquare(
                Board.coordToSquare(coord), this.transCode, true));
    }, this);
    // Рассчитать оценку обязательных ходов
    var layout = new Layout(moves);
    // При автоматических партиях ищем комбинации завершения до 35 ходов
    var computetarget = this.el.querySelector('#automove').checked ? 35 : 225;
    var vertex = Vertex.estimate(layout,
            this.record.length % 2 ? Layout.WHITE : Layout.BLACK, true, computetarget);
    if (vertex.state === 0) {
        vertex = Vertex.estimate(layout,
                this.record.length % 2 ? Layout.BLACK : Layout.WHITE, true, computetarget);
    }
    if (vertex.state === 0) {
        if (vertex.edges === null || vertex.edges.length === 0) {
            // Подбираем простые ходы для скрытой атаки
            moves = layout.gains(Pattern.ONE).concat(layout.downs(Pattern.TWO));
            // Рассчитаем полученные позиции
            vertex.edges = [];
            moves.some(function (square) {
                // Создать ребро с вершиной 
                var e = this.makeEdge(square);
                // e.vertex.state = 0;
                this.edges.push(e);
            }, vertex);
            // Сортировка дочерних узлов по рейтингу
            vertex.edges.sort(Edge.Comparator);
            // Оставляем позиции с лучшим ходом
            var es = [];
            var top = vertex.edges[0].vertex;
            vertex.edges.some(function (edge) {
                if (edge.vertex.state !== top.state
                        || edge.vertex.rating !== top.rating) {
                    return true;
                }
                es.push(edge);
            }, vertex);
            vertex.edges = es;
        }
        vertex.edges.forEach(function (edge) {
            this.current[Board.squareToCoord(edge.square)] = 226;
        }, this);
        return;
    }
    // Если предопределено развитие позиции
    if (vertex.edges === null) {
        this.current.count = Math.abs(vertex.state);
        if (this.current.count === 1) {
            // Определить поля выигрышной фигуры
            var top = vertex.top(), moves = top.moves(), coords = [];
            moves.forEach(function (move) {
                coords.push(Board.squareToCoord(
                        Layout.transSquare(move, this.transCode)));
            }, this);
            // Победа
            this.current.coords = coords;
            this.checkwin();
            return;
        } else {
            // Добавить ходы решения
            vertex.moves().forEach(function (move) {
                this.current[Board.squareToCoord(move)] =
                        this.current.count - 1;
            }, this);
            return;
        }
    }
    // Преобразовать граф решений в список ходов
    Board.vertexToSolution(vertex, this.current);
};

// Подсчет рещений
Board.count = function (current) {
    if (typeof current === 'object')
        return Board.fillCount(current) - 1;
    else if (typeof current !== 'undefined')
        return  current - 1;
    else
        return 225;
};

// Подсчет рещений
Board.prototype.count = function () {
    return Board.count(this.current);
};

// Подсчет количество оставщихся ходов для позиции
Board.fillCount = function (current) {
    if (typeof current.count === 'undefined') {
        var maxcount = 0;
        for (var coord in current) {
            var count = current[coord];
            // Рекурсия
            if (typeof count === 'object') {
                count = Board.fillCount(count);
            }
            if (count > maxcount) {
                maxcount = count;
            }
        }
        current.count = maxcount + 1;
    }
    return current.count;
};

// Определить номер трансформации по 2-му ходу
Board.squareTransCode = function (square) {
    var offset = square & 0xf, number = square >> 4;
    // console.log('[' + number + ':' + offset + ']');
    return number <= 7 ? (offset <= 7 ? (offset <= number ? 7 : 6)
            : (14 - offset <= number ? 5 : 0))
            : (offset <= 7 ? (offset <= 14 - number ? 1 : 4)
                    : (14 - offset <= 14 - number ? 3 : 2));
};

// Определить номер трансформации по координате 2-го хода
Board.coordTransCode = function (coord) {
    return Board.squareTransCode(Board.coordToSquare(coord));
};

// Преобразование номера ячейки в координаты
Board.squareToCoord = function (square) {
    return String.fromCharCode('a'.charCodeAt(0) + (square & 0xf))
            + (15 - (square >> 4));
};

// Обратное преобразование координат в номер ячейки
Board.coordToSquare = function (coord) {
    return ((15 - parseInt(coord.substring(1))) << 4)
            | (coord.charCodeAt(0) - 'a'.charCodeAt(0));
};

// Трансформация координаты
Board.prototype.transCoord = function (coord, back) {
    return Board.squareToCoord(Layout.transSquare(
            Board.coordToSquare(coord), this.transCode, back));
};

// Рекурсивное преобразование вершин в набор ходов
Board.vertexToSolution = function (vertex, current) {
    if (vertex.edges !== null) {
        current = current || {};
        vertex.edges.forEach(function (edge) {
            current[Board.squareToCoord(edge.square)] =
                    Board.vertexToSolution(edge.vertex);
        }, this);
        return current;
    } else {
        return Math.abs(vertex.state);
    }
};

// Показать динию прогресса
Board.prototype.showProgress = function () {
    this.el.querySelector('.line-progress').className = 'line-progress active';
};

// Спрятать динию прогресса
Board.prototype.hideProgress = function () {
    this.el.querySelector('.line-progress').className = 'line-progress';
};

// Замена хода
Board.prototype.swap = async function () {
    this.attacker = this.attacker === Layout.BLACK ? Layout.WHITE : Layout.BLACK;
    await this.auto();
};

// Следующий ход - два автоматических хода
Board.prototype.next = async function () {
    await this.swap();
    await this.swap();
};

// Оторазить доску
window.board = new Board(document.getElementById('board'));
