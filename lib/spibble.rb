require 'fileutils'

%w[database lastfm import input].each {|r| require "spibble/#{r}" }

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

    def list
      puts @database.map {|a| "#{a.artist} - #{a.title}" }.sort
    end

    def input_sides!(sides)
      # TODO: Allow arbitrary side naming
      puts 'Sides:'
      sides[1] = 'A'
      puts ' A: 1'

      side = 'B'
      loop do
        number = Input.line(" #{side}: ")
        break if number.empty?
        number = number.to_i
        unless number == 0 || sides.include?(number)
          sides[number] = side.dup
          side.succ!
        end
      end
    end

    def add
    end

    def import(files)
      begin
        if files.length == 1
          album = Import.directory(File.expand_path(files.first))
        else
          album = Import.files(files.map {|f| File.expand_path(f) })
        end
      rescue Import::ImportError => e
        puts "Error: #{e}"
        exit 1
      end

      puts album
      puts

      input_sides!(album.sides)

      puts
      puts album
      @database.add(album)
    end

    def scrobble(album, offset = 1)
      album = @database.find(album)
      unless album
        puts 'Error: could not find album in database'
        exit 1
      end

      if offset.is_a? String
        regex = /#{Regexp.escape(offset)}/i
        side = album.sides.select {|n, s| s =~ regex }
        if side.empty?
          puts 'Error: no such side'
          exit 1
        end
        offset = side.keys.first
      end

      puts album
      puts

      unless album.sides.include?(offset)
        scrobble = Input.yesno("Scrobble from track #{album.tracks[offset - 1]}? ")
      end

      album.tracks.drop(offset - 1).each do |track|
        if side = album.sides[track.number]
          scrobble = Input.yesno("Scrobble#{' side' unless side.length > 2} #{side}? ")
        end

        if scrobble
          puts " #{track}"
          @lastfm.now_playing(album, track)
          sleep(track.length)
          @lastfm.scrobble
        end
      end
    end
  end
end
