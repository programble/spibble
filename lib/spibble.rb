require 'fileutils'

%w[database lastfm import].each {|r| require "spibble/#{r}" }

module Spibble
  SPIBBLE_DIR = File.join(Dir.home, '.config', 'spibble')

  class Application
    def initialize
      FileUtils.mkdir_p(SPIBBLE_DIR)

      @database = Database.new(File.join(SPIBBLE_DIR, 'albums.yml'))

      begin
        @lastfm = Lastfm.new(File.join(SPIBBLE_DIR, 'session'))
      rescue Lastfm::ApiError => e
        puts "Error: #{e}"
        exit 1
      end
    end

    def input_yesno(prompt = '', default = true)
      loop do
        print "#{prompt} "
        input = STDIN.gets.chomp
        return default if input.empty?
        return true if input[0].downcase == ?y
        return false if input[0].downcase == ?n
      end
    end

    def list
      puts @database.map {|a| "#{a.artist} - #{a.title}" }.sort
    end

    def add
    end

    def import(files)
    end

    def scrobble(album)
      album = @database.find(album)
      unless album
        puts 'Error: could not find album in database'
        exit 1
      end

      puts album
      puts

      scrobble = true
      album.tracks.each do |track|
        if side = album.sides[track.number]
          scrobble = input_yesno("Scrobble#{' side' unless side.length > 2} #{side}?")
        end

        if scrobble
          puts track
          @lastfm.now_playing(album, track)
          sleep(track.length)
          @lastfm.scrobble
        end
      end
    end
  end
end
