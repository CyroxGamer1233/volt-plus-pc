//! A small original lane-crossing game shown while the Minecraft installer runs.
use std::time::Instant;

pub(super) struct RoadGame {
    pub x: i32,
    pub row: i32,
    pub best: i32,
    pub dead: bool,
    pub time: f32,
    last: Instant,
}
impl Default for RoadGame {
    fn default() -> Self {
        Self { x: 4, row: 0, best: 0, dead: false, time: 0.0, last: Instant::now() }
    }
}
impl RoadGame {
    pub fn move_by(&mut self, dx: i32, dy: i32) {
        if self.dead { return; }
        self.x = (self.x + dx).clamp(0, 8);
        self.row = (self.row + dy).max(0);
        self.best = self.best.max(self.row);
        self.collide();
    }
    pub fn tick(&mut self, focused: bool) {
        let now = Instant::now();
        let dt = now.duration_since(self.last).as_secs_f32().min(0.05);
        self.last = now;
        if focused && !self.dead { self.time += dt; self.collide(); }
    }
    pub fn road(row: i32) -> bool { row > 0 && row % 4 != 0 }
    /// Each road has two moving cars, always wrapping the full lane.
    pub fn car_x(&self, row: i32, car: i32) -> f32 {
        let direction = if row % 2 == 0 { 1.0 } else { -1.0 };
        let speed = 0.75 + (row.rem_euclid(3) as f32) * 0.19;
        (row as f32 * 2.17 + car as f32 * 5.5 + self.time * speed * direction).rem_euclid(11.0) - 1.0
    }
    fn collide(&mut self) {
        if !Self::road(self.row) { return; }
        for car in 0..2 {
            if (self.car_x(self.row, car) - self.x as f32).abs() < 0.70 {
                self.dead = true;
                break;
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test] fn movement_is_bounded_and_restart_resets() {
        let mut game = RoadGame::default();
        game.move_by(-100, -100);
        assert_eq!((game.x, game.row), (0, 0));
        game.move_by(100, 0);
        assert_eq!(game.x, 8);
        let reset = RoadGame::default();
        assert!(!reset.dead);
        assert_eq!(reset.best, 0);
    }
}
