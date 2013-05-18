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

    def list
      @database.each do |album|
        puts "#{album.artist} - #{album.title}"
      end
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

      album.tracks.each do |track|
        if side = album.sides[track.number]
          print "Scrobble#{' side' unless side[' ']} #{side}? "
          STDIN.gets # TODO: Handle input
        end

        puts track
        @lastfm.now_playing(album, track)
        sleep(track.length)
        @lastfm.scrobble
      end
    end
  end
end
