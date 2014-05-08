#define MASTER 0
#define FORWARD_MOTION_MAILBOX 1
#define BT_PERIOD_MS 50

#define DEADLINE_UPPER_BOUND 1000

int n = 0;
int received_forward_velicity = 0;
int forward_velocity = 0;
long valid_until = 0;
string last_payload;

unsigned long start_tick;
bool bt_handshake = false;

task btComm() {
  string payload;
  int delimiter_pos;

  if(ReceiveRemoteString(FORWARD_MOTION_MAILBOX, true, payload) == NO_ERR) {
    if (!bt_handshake) {
      start_tick = CurrentTick();
      bt_handshake = true;
    }
    n++;

    if(delimiter_pos = Pos(" ", payload) != -1) {
      received_forward_velicity = atoi(payload);
      valid_until = start_tick + atol(RightStr(payload, StrLen(payload) - delimiter_pos - 2));
    }
  }
}

task btController() {
  if(bt_handshake) {
    long relative_deadline = valid_until - CurrentTick();

    if(relative_deadline > 0) {
      if(relative_deadline < DEADLINE_UPPER_BOUND)
        forward_velocity = received_forward_velicity;
      else
        forward_velocity = 0;
    } else {
      forward_velocity = 0;
    }
  }
}

task btDebug() {
  ClearLine(LCD_LINE2);
  TextOut(0, LCD_LINE2, "#" + NumToStr(n) + ": " + last_payload);

  ClearLine(LCD_LINE4);
  TextOut(0, LCD_LINE4, "V: " + NumToStr(forward_velocity));
  ClearLine(LCD_LINE5);
  TextOut(0, LCD_LINE5, "S: " + NumToStr(start_tick));
  ClearLine(LCD_LINE6);
  TextOut(0, LCD_LINE6, "T: " + NumToStr(valid_until));
}
