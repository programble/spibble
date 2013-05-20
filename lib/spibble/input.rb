require 'highline'

module Spibble
  module Input
    module_function

    def yesno(prompt = '', default = true)
      print prompt
      input = raw("ynYN\n").downcase
      puts input
      return default if input == "\n"
      input == ?y
    end

    def raw(chars = '')
      # Hijack HighLine's mode changing code
      h = HighLine.new
      h.raw_no_echo_mode
      begin
        loop do
          c = STDIN.getc
          return c if chars.empty? || chars[c]
        end
      ensure
        h.restore_mode
      end
    end

    def line(prompt = '', default = '', newline = true, ii = nil)
      buf = default
      i = ii || buf.length
      print "#{prompt}#{default}"
      print "\b" * (buf.length - i)

      # Hijack HighLine's mode changing code
      h = HighLine.new
      h.raw_no_echo_mode
      begin
        loop do
          c = STDIN.getc
          if c == "\e" # Get the rest of the escape sequence
            c << STDIN.getc << STDIN.getc # There are at least two more bytes
            c << STDIN.getc while (?0..?9).include? c[-1] # Keep getting numbers until ~
          end

          case c
          when "\n" # Enter
            puts if newline
            return buf
          when "\e[D" # Left arrow
            next if i == 0
            i -= 1
            print c # Echo moves cursor
          when "\e[C" # Right Arrow
            next if i == buf.length
            i += 1
            print c # Echo moves cursor
          when "\eOH", "\x01" # Home
            print "\b" * i
            i = 0
          when "\eOF", "\x05" # End
            print "\e[C" * (buf.length - i) # Simulate right arrow
            i = buf.length
          when "\b", "\x7F" # Backspace
            next if i == 0
            buf = buf[0, i - 1] + buf[i..-1] # Delete char before cursor
            i -= 1
            print "\b" + buf[i..-1].to_s + ' ' # Move rest of buf left one
            print "\b" * (buf.length - i + 1) # Move cursor back
          when "\e[3~" # Delete
            next if buf.empty? || i == buf.length
            buf = buf[0, i] + buf[(i + 1)..-1] # Delete char under cursor
            print buf[i..-1] + ' ' # Reprint rest of buf
            print "\b" * (buf.length - i + 1) # Move cursor back
          when /\e/, "\t"
            next # Ignore other escapes
          else
            buf.insert(i, c)
            print buf[i..-1]
            i += 1
            print "\b" * (buf.length - i) # Move cursor back
          end
        end
      ensure
        h.restore_mode
      end
    end
  end
end
