require 'yaml'

module Spibble
  Album = Struct.new(:title, :artist, :tracks)
  Track = Struct.new(:number, :title, :length)

  class Database
    def initialize(filename)
      @filename = filename
      load
    end

    def load
      @db = {}
      if File.exist? @filename
        File.open(@filename) do |f|
          YAML.load(f).each do |title, album|
            tracks = []
            album['tracks'].each_with_index do |t, i|
              tracks << t ? Track.new(i + 1, t.keys.first, t.values.first) : t
            end
            @db[title] = Album.new(title, album['artist'], tracks)
          end
        end
      end
    end

    def save
      File.open(@filename, 'w') do |f|
        hash = {}
        @db.values.each do |album|
          hash[album.title] = {'artist' => album.artist, 'tracks' => album.tracks.map {|t| {t.title => t.length} }}
        end
        YAML.dump(hash, f)
      end
    end

    def add(album)
      @db[album.title] = album
      save
    end

    def find(title)
      regex = /#{Regexp.escape(title)}/i
      key = @db.keys.find {|k| k =~ regex }
      key ? @db[key] : key
    end
  end
end
